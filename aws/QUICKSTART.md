# Quick Start: Deploy Wish Keeper to AWS

This guide walks you through deploying the Wish Keeper application to AWS with CI/CD pipeline.

## Prerequisites

1. **AWS Account** with appropriate permissions
2. **AWS CLI** configured with credentials
3. **GitHub Repository** with this code
4. **PostgreSQL Client** (`psql`) installed locally

## Step-by-Step Deployment

### Phase 1: Setup AWS Infrastructure

#### 1. Create VPC and Networking (10-15 minutes)

```bash
cd aws/cloudformation

# Deploy VPC
aws cloudformation create-stack \
  --stack-name wish-keeper-vpc \
  --template-body file://vpc.yml \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev

# Wait for completion
aws cloudformation wait stack-create-complete \
  --stack-name wish-keeper-vpc

# Get outputs
aws cloudformation describe-stacks \
  --stack-name wish-keeper-vpc \
  --query 'Stacks[0].Outputs'
```

**Save these outputs:**
- VPCId
- PrivateSubnet1Id, PrivateSubnet2Id
- PublicSubnet1Id, PublicSubnet2Id

#### 2. Create ECR Repository (2 minutes)

```bash
# Deploy ECR
aws cloudformation create-stack \
  --stack-name wish-keeper-ecr \
  --template-body file://ecr.yml

# Wait for completion
aws cloudformation wait stack-create-complete \
  --stack-name wish-keeper-ecr

# Get repository URI
export ECR_REPO=$(aws cloudformation describe-stacks \
  --stack-name wish-keeper-ecr \
  --query 'Stacks[0].Outputs[?OutputKey==`RepositoryUri`].OutputValue' \
  --output text)

echo "ECR Repository: $ECR_REPO"
```

#### 3. Create ECS Security Group (temporary, until ECS stack is created)

```bash
# Get VPC ID
export VPC_ID=$(aws cloudformation describe-stacks \
  --stack-name wish-keeper-vpc \
  --query 'Stacks[0].Outputs[?OutputKey==`VPCId`].OutputValue' \
  --output text)

# Create security group for ECS tasks
export ECS_SG=$(aws ec2 create-security-group \
  --group-name wish-keeper-ecs-sg \
  --description "Security group for Wish Keeper ECS tasks" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

echo "ECS Security Group: $ECS_SG"
```

#### 4. Create RDS Database (15-20 minutes) - **FIRST CRITICAL STEP**

```bash
# Get private subnet IDs
export PRIVATE_SUBNET_1=$(aws cloudformation describe-stacks \
  --stack-name wish-keeper-vpc \
  --query 'Stacks[0].Outputs[?OutputKey==`PrivateSubnet1Id`].OutputValue' \
  --output text)

export PRIVATE_SUBNET_2=$(aws cloudformation describe-stacks \
  --stack-name wish-keeper-vpc \
  --query 'Stacks[0].Outputs[?OutputKey==`PrivateSubnet2Id`].OutputValue' \
  --output text)

# Deploy RDS (choose a strong password!)
aws cloudformation create-stack \
  --stack-name wish-keeper-rds \
  --template-body file://rds.yml \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=DBPassword,ParameterValue=YourStrongPassword123! \
    ParameterKey=VPCId,ParameterValue=$VPC_ID \
    ParameterKey=PrivateSubnetIds,ParameterValue=\"$PRIVATE_SUBNET_1,$PRIVATE_SUBNET_2\" \
    ParameterKey=AllowedSecurityGroups,ParameterValue=$ECS_SG

# Wait for RDS creation (this takes ~15 minutes)
aws cloudformation wait stack-create-complete \
  --stack-name wish-keeper-rds

# Get RDS endpoint
export DB_HOST=$(aws cloudformation describe-stacks \
  --stack-name wish-keeper-rds \
  --query 'Stacks[0].Outputs[?OutputKey==`DBEndpoint`].OutputValue' \
  --output text)

echo "RDS Endpoint: $DB_HOST"
```

#### 5. Apply Database Schema to RDS

```bash
# Navigate to scripts directory
cd ../scripts

# Run migration script
./migrate-to-rds.sh wish-keeper-rds

# The script will:
# - Connect to RDS
# - Apply People.pgsql schema
# - Apply wishes.pgsql schema
# - Verify tables were created
```

**Manual method (if script fails):**

```bash
# Get DB password from Secrets Manager
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id wish-keeper-rds-db-credentials \
  --query 'SecretString' \
  --output text | jq -r '.password')

# Apply schema manually
cd ../../src/main/resources
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U wishkeeper -d webapp_db -f People.pgsql
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U wishkeeper -d webapp_db -f wishes.pgsql

# Verify
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -U wishkeeper -d webapp_db -c "\dt"
```

### Phase 2: Setup CI/CD Pipeline

#### 6. Create GitHub Connection

You need to create a CodeStar connection to GitHub:

```bash
# Create connection (will be in PENDING state)
aws codestar-connections create-connection \
  --provider-type GitHub \
  --connection-name wish-keeper-github

# Get the ARN (save this!)
export GITHUB_CONNECTION_ARN=$(aws codestar-connections list-connections \
  --query 'Connections[?ConnectionName==`wish-keeper-github`].ConnectionArn' \
  --output text)

echo "GitHub Connection ARN: $GITHUB_CONNECTION_ARN"
```

**Important:** You must complete the GitHub connection in AWS Console:
1. Go to: https://console.aws.amazon.com/codesuite/settings/connections
2. Find "wish-keeper-github" connection
3. Click "Update pending connection"
4. Authenticate with GitHub and authorize access

#### 7. Create ECS Cluster (placeholder for now)

```bash
# Create basic ECS cluster
aws ecs create-cluster \
  --cluster-name wish-keeper-cluster \
  --capacity-providers FARGATE FARGATE_SPOT

export ECS_CLUSTER=wish-keeper-cluster
```

#### 8. Create IAM Roles for ECS

```bash
# Create ECS Task Execution Role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# Add Secrets Manager access
aws iam put-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-name SecretsManagerAccess \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": ["secretsmanager:GetSecretValue"],
      "Resource": "*"
    }]
  }'

# Create ECS Task Role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": {"Service": "ecs-tasks.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }]
  }'
```

#### 9. Create Initial ECS Service (minimal)

First, we need to push an initial image and create a task definition:

```bash
# Build and push initial image locally
cd ../../..  # Back to project root

# Login to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ECR_REPO

# Build and push
mvn clean package -DskipTests
docker build -t wish-keeper .
docker tag wish-keeper:latest $ECR_REPO:latest
docker push $ECR_REPO:latest

# Create task definition
export DB_PASSWORD_SECRET_ARN=$(aws cloudformation describe-stacks \
  --stack-name wish-keeper-rds \
  --query 'Stacks[0].Outputs[?OutputKey==`DBPasswordSecretArn`].OutputValue' \
  --output text)

aws ecs register-task-definition \
  --family wish-keeper-task \
  --network-mode awsvpc \
  --requires-compatibilities FARGATE \
  --cpu 512 \
  --memory 1024 \
  --execution-role-arn arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):role/ecsTaskExecutionRole \
  --task-role-arn arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):role/ecsTaskRole \
  --container-definitions "[
    {
      \"name\": \"wish-keeper-app\",
      \"image\": \"$ECR_REPO:latest\",
      \"essential\": true,
      \"portMappings\": [{\"containerPort\": 8000, \"protocol\": \"tcp\"}],
      \"environment\": [
        {\"name\": \"DB_HOST\", \"value\": \"$DB_HOST\"},
        {\"name\": \"DB_PORT\", \"value\": \"5432\"},
        {\"name\": \"DB_NAME\", \"value\": \"webapp_db\"},
        {\"name\": \"DB_USER\", \"value\": \"wishkeeper\"}
      ],
      \"secrets\": [
        {\"name\": \"DB_PASSWORD\", \"valueFrom\": \"$DB_PASSWORD_SECRET_ARN\"}
      ],
      \"logConfiguration\": {
        \"logDriver\": \"awslogs\",
        \"options\": {
          \"awslogs-group\": \"/ecs/wish-keeper\",
          \"awslogs-region\": \"us-east-1\",
          \"awslogs-stream-prefix\": \"ecs\",
          \"awslogs-create-group\": \"true\"
        }
      }
    }
  ]"

# Create ECS service
aws ecs create-service \
  --cluster wish-keeper-cluster \
  --service-name wish-keeper-service \
  --task-definition wish-keeper-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={
    subnets=[$PRIVATE_SUBNET_1,$PRIVATE_SUBNET_2],
    securityGroups=[$ECS_SG],
    assignPublicIp=DISABLED
  }"
```

#### 10. Deploy CodePipeline

```bash
cd aws/cloudformation

# Deploy pipeline
aws cloudformation create-stack \
  --stack-name wish-keeper-pipeline \
  --template-body file://codepipeline.yml \
  --parameters \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=GitHubOwner,ParameterValue=YOUR_GITHUB_USERNAME \
    ParameterKey=GitHubRepo,ParameterValue=wish-keeper \
    ParameterKey=GitHubBranch,ParameterValue=main \
    ParameterKey=GitHubConnectionArn,ParameterValue=$GITHUB_CONNECTION_ARN \
    ParameterKey=ECRRepositoryName,ParameterValue=wish-keeper \
    ParameterKey=ECSClusterName,ParameterValue=wish-keeper-cluster \
    ParameterKey=ECSServiceName,ParameterValue=wish-keeper-service \
    ParameterKey=DBHost,ParameterValue=$DB_HOST \
    ParameterKey=DBPasswordSecretArn,ParameterValue=$DB_PASSWORD_SECRET_ARN \
  --capabilities CAPABILITY_NAMED_IAM

# Wait for completion
aws cloudformation wait stack-create-complete \
  --stack-name wish-keeper-pipeline

# Get pipeline URL
aws cloudformation describe-stacks \
  --stack-name wish-keeper-pipeline \
  --query 'Stacks[0].Outputs[?OutputKey==`PipelineUrl`].OutputValue' \
  --output text
```

### Phase 3: Verify Deployment

#### 11. Test the Pipeline

```bash
# Check pipeline status
aws codepipeline get-pipeline-state \
  --name wish-keeper-pipeline-pipeline

# View CodeBuild logs
aws logs tail /aws/codebuild/wish-keeper-pipeline --follow

# Check ECS service
aws ecs describe-services \
  --cluster wish-keeper-cluster \
  --services wish-keeper-service
```

#### 12. Test the Application

```bash
# Get task private IP (for testing from within VPC)
TASK_ARN=$(aws ecs list-tasks \
  --cluster wish-keeper-cluster \
  --service-name wish-keeper-service \
  --query 'taskArns[0]' \
  --output text)

aws ecs describe-tasks \
  --cluster wish-keeper-cluster \
  --tasks $TASK_ARN
```

## Next Steps

1. **Add Load Balancer** - Create ALB for public access
2. **Setup Auto-scaling** - Configure ECS auto-scaling policies
3. **Add Monitoring** - Setup CloudWatch dashboards and alarms
4. **Configure DNS** - Setup Route53 for custom domain
5. **Add SSL/TLS** - Request ACM certificate for HTTPS

## Troubleshooting

### Pipeline Fails to Build

```bash
# Check CodeBuild logs
aws logs tail /aws/codebuild/wish-keeper-pipeline --follow

# Check buildspec.yml is in repository root
```

### ECS Tasks Not Starting

```bash
# Check task logs
aws logs tail /ecs/wish-keeper --follow

# Check security group rules
aws ec2 describe-security-groups --group-ids $ECS_SG

# Verify RDS connectivity
aws rds describe-db-instances --db-instance-identifier wish-keeper-rds-postgres
```

### Database Connection Issues

```bash
# Test connection from within VPC (use EC2 bastion or ECS Exec)
psql -h $DB_HOST -U wishkeeper -d webapp_db

# Check security group rules allow ECS -> RDS on port 5432
```

## Cost Optimization Tips

1. Use Fargate Spot for non-production
2. Enable RDS autoscaling
3. Use single NAT Gateway for dev
4. Setup CloudWatch log retention policies
5. Enable S3 lifecycle policies for artifacts

## Clean Up

To delete all resources:

```bash
# Delete in reverse order
aws cloudformation delete-stack --stack-name wish-keeper-pipeline
aws cloudformation delete-stack --stack-name wish-keeper-rds
aws ecs delete-service --cluster wish-keeper-cluster --service wish-keeper-service --force
aws ecs delete-cluster --cluster wish-keeper-cluster
aws cloudformation delete-stack --stack-name wish-keeper-ecr
aws cloudformation delete-stack --stack-name wish-keeper-vpc
```
