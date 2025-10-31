# AWS Deployment Checklist

Use this checklist to track your deployment progress. Check off items as you complete them.

## 📋 Pre-Deployment Checklist

### Prerequisites
- [ ] AWS Account created and active
- [ ] AWS CLI installed (`aws --version`)
- [ ] AWS CLI configured (`aws configure`)
- [ ] PostgreSQL client installed (`psql --version`)
- [ ] Docker installed (`docker --version`)
- [ ] Maven installed (`mvn --version`)
- [ ] jq installed (`jq --version`)
- [ ] Git repository on GitHub
- [ ] IAM permissions verified (Admin or PowerUser)

### Environment Preparation
- [ ] Decided on AWS region (default: us-east-1)
- [ ] Decided on environment name (dev/staging/prod)
- [ ] Generated strong database password
- [ ] Review cost estimates (~$90/month dev, ~$270/month prod)
- [ ] Budget alerts configured (optional)

## 🎯 Phase 1: Database Migration to RDS

### Step 1: Deploy VPC (10 minutes)
- [ ] Navigate to `aws/cloudformation` directory
- [ ] Deploy VPC stack:
  ```bash
  aws cloudformation create-stack \
    --stack-name wish-keeper-vpc \
    --template-body file://vpc.yml
  ```
- [ ] Wait for stack creation:
  ```bash
  aws cloudformation wait stack-create-complete \
    --stack-name wish-keeper-vpc
  ```
- [ ] Verify outputs:
  - [ ] VPC ID saved
  - [ ] Public Subnet IDs saved
  - [ ] Private Subnet IDs saved
- [ ] Estimated time: ⏱️ 10 minutes

### Step 2: Create Security Group for ECS
- [ ] Get VPC ID from stack output
- [ ] Create ECS security group:
  ```bash
  aws ec2 create-security-group \
    --group-name wish-keeper-ecs-sg \
    --description "Security group for Wish Keeper ECS tasks" \
    --vpc-id <VPC_ID>
  ```
- [ ] Save security group ID

### Step 3: Deploy RDS Database (20 minutes) ⭐
- [ ] Copy parameter template:
  ```bash
  cp rds-params.json.example rds-params.json
  ```
- [ ] Edit `rds-params.json` with your values:
  - [ ] VPC ID
  - [ ] Private subnet IDs (comma-separated)
  - [ ] ECS security group ID
  - [ ] Strong database password
  - [ ] Database instance class (db.t3.micro for dev)
  - [ ] Multi-AZ setting (false for dev, true for prod)
- [ ] Deploy RDS stack:
  ```bash
  aws cloudformation create-stack \
    --stack-name wish-keeper-rds \
    --template-body file://rds.yml \
    --parameters file://rds-params.json
  ```
- [ ] Wait for RDS creation (this takes ~15-20 minutes):
  ```bash
  aws cloudformation wait stack-create-complete \
    --stack-name wish-keeper-rds
  ```
- [ ] Verify outputs:
  - [ ] RDS endpoint address saved
  - [ ] Secret ARN saved
  - [ ] Database name confirmed
- [ ] Estimated time: ⏱️ 20 minutes

### Step 4: Migrate Database Schema (2 minutes) ⭐
- [ ] Navigate to `aws/scripts` directory
- [ ] Make script executable:
  ```bash
  chmod +x migrate-to-rds.sh
  ```
- [ ] Run migration script:
  ```bash
  ./migrate-to-rds.sh wish-keeper-rds
  ```
- [ ] Verify output shows:
  - [ ] Connection successful
  - [ ] People table created
  - [ ] Wishes table created
  - [ ] Table count >= 2
- [ ] (Optional) Test connection manually:
  ```bash
  psql -h <RDS_ENDPOINT> -U wishkeeper -d webapp_db -c "\dt"
  ```
- [ ] Estimated time: ⏱️ 2 minutes

**✅ MILESTONE: Database is now in AWS RDS!**

## 🚀 Phase 2: CI/CD Pipeline Setup

### Step 5: Create ECR Repository (2 minutes)
- [ ] Navigate to `aws/cloudformation` directory
- [ ] Deploy ECR stack:
  ```bash
  aws cloudformation create-stack \
    --stack-name wish-keeper-ecr \
    --template-body file://ecr.yml
  ```
- [ ] Wait for completion:
  ```bash
  aws cloudformation wait stack-create-complete \
    --stack-name wish-keeper-ecr
  ```
- [ ] Save ECR repository URI from outputs
- [ ] Estimated time: ⏱️ 2 minutes

### Step 6: Connect GitHub (5 minutes)
- [ ] Create CodeStar connection:
  ```bash
  aws codestar-connections create-connection \
    --provider-type GitHub \
    --connection-name wish-keeper-github
  ```
- [ ] Save connection ARN
- [ ] Open AWS Console: https://console.aws.amazon.com/codesuite/settings/connections
- [ ] Find "wish-keeper-github" connection
- [ ] Click "Update pending connection"
- [ ] Authorize with GitHub
- [ ] Verify connection status is "AVAILABLE"
- [ ] Estimated time: ⏱️ 5 minutes

### Step 7: Create ECS Cluster (2 minutes)
- [ ] Create ECS cluster:
  ```bash
  aws ecs create-cluster \
    --cluster-name wish-keeper-cluster \
    --capacity-providers FARGATE FARGATE_SPOT
  ```
- [ ] Verify cluster is active
- [ ] Estimated time: ⏱️ 2 minutes

### Step 8: Create IAM Roles (3 minutes)
- [ ] Create ECS Task Execution Role:
  ```bash
  aws iam create-role \
    --role-name ecsTaskExecutionRole \
    --assume-role-policy-document file://ecs-task-execution-role-policy.json
  ```
- [ ] Attach managed policy:
  ```bash
  aws iam attach-role-policy \
    --role-name ecsTaskExecutionRole \
    --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
  ```
- [ ] Add Secrets Manager access
- [ ] Create ECS Task Role
- [ ] Estimated time: ⏱️ 3 minutes

### Step 9: Build and Push Initial Image (5 minutes)
- [ ] Navigate to project root
- [ ] Login to ECR:
  ```bash
  aws ecr get-login-password | docker login --username AWS --password-stdin <ECR_URI>
  ```
- [ ] Build application:
  ```bash
  mvn clean package -DskipTests
  ```
- [ ] Build Docker image:
  ```bash
  docker build -t wish-keeper .
  ```
- [ ] Tag image:
  ```bash
  docker tag wish-keeper:latest <ECR_URI>:latest
  ```
- [ ] Push to ECR:
  ```bash
  docker push <ECR_URI>:latest
  ```
- [ ] Estimated time: ⏱️ 5 minutes

### Step 10: Create ECS Service (5 minutes)
- [ ] Register task definition
- [ ] Create ECS service:
  ```bash
  aws ecs create-service \
    --cluster wish-keeper-cluster \
    --service-name wish-keeper-service \
    --task-definition wish-keeper-task \
    --desired-count 1 \
    --launch-type FARGATE
  ```
- [ ] Verify service is running
- [ ] Check task is in RUNNING state
- [ ] Estimated time: ⏱️ 5 minutes

### Step 11: Deploy CodePipeline (5 minutes)
- [ ] Gather all required parameters:
  - [ ] GitHub owner/username
  - [ ] GitHub repository name
  - [ ] GitHub branch (usually "main")
  - [ ] GitHub connection ARN
  - [ ] ECR repository name
  - [ ] ECS cluster name
  - [ ] ECS service name
  - [ ] RDS endpoint
  - [ ] DB password secret ARN
- [ ] Deploy pipeline stack:
  ```bash
  aws cloudformation create-stack \
    --stack-name wish-keeper-pipeline \
    --template-body file://codepipeline.yml \
    --parameters <see QUICKSTART.md> \
    --capabilities CAPABILITY_NAMED_IAM
  ```
- [ ] Wait for completion
- [ ] Get pipeline URL from outputs
- [ ] Open pipeline in AWS Console
- [ ] Estimated time: ⏱️ 5 minutes

**✅ MILESTONE: CI/CD Pipeline is configured!**

## 🎉 Phase 3: Verification & Testing

### Step 12: Test Pipeline (5 minutes)
- [ ] Make a small code change (e.g., update README)
- [ ] Commit and push to GitHub:
  ```bash
  git add .
  git commit -m "Test pipeline"
  git push
  ```
- [ ] Watch pipeline execution in AWS Console
- [ ] Verify Source stage completes
- [ ] Verify Build stage completes
- [ ] Verify Deploy stage completes
- [ ] Check ECS tasks are updated
- [ ] Estimated time: ⏱️ 5 minutes

### Step 13: Verify Application (5 minutes)
- [ ] Get task private IP or setup ALB (optional)
- [ ] Test health endpoint:
  ```bash
  curl http://<endpoint>/api/hello
  ```
- [ ] Test creating a person:
  ```bash
  curl -X POST http://<endpoint>/api/people \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Test","lastName":"User"}'
  ```
- [ ] Test creating a wish
- [ ] Verify data persists in RDS
- [ ] Estimated time: ⏱️ 5 minutes

### Step 14: Check Logs (2 minutes)
- [ ] View ECS task logs in CloudWatch:
  ```bash
  aws logs tail /ecs/wish-keeper --follow
  ```
- [ ] Verify no errors
- [ ] Check database connections successful
- [ ] Estimated time: ⏱️ 2 minutes

**✅ MILESTONE: Application is running in AWS!**

## 📊 Post-Deployment

### Monitoring Setup
- [ ] CloudWatch dashboard created (optional)
- [ ] CloudWatch alarms verified:
  - [ ] RDS high CPU alarm exists
  - [ ] RDS low storage alarm exists
  - [ ] Pipeline failure alarm exists
- [ ] SNS topic for notifications (optional)
- [ ] Log retention periods set

### Security Review
- [ ] Security groups reviewed and minimal
- [ ] RDS is not publicly accessible
- [ ] Secrets are in Secrets Manager (not environment variables)
- [ ] IAM roles follow least privilege
- [ ] VPC flow logs enabled (optional)

### Documentation
- [ ] RDS endpoint documented
- [ ] ECR repository URI documented
- [ ] Pipeline URL documented
- [ ] Deployment runbook updated
- [ ] Architecture diagram updated (optional)

### Cost Optimization
- [ ] Review RDS instance size
- [ ] Consider Fargate Spot for dev
- [ ] Set up cost alerts
- [ ] Review CloudWatch log retention
- [ ] Consider single NAT gateway for dev

## 🔄 Ongoing Operations

### Regular Tasks
- [ ] Monitor CloudWatch alarms weekly
- [ ] Review AWS costs monthly
- [ ] Update dependencies monthly
- [ ] Test disaster recovery quarterly
- [ ] Review security settings quarterly

### Backup & Recovery
- [ ] RDS automated backups enabled (verify)
- [ ] Backup retention period: 7 days (prod)
- [ ] Test RDS snapshot restore (quarterly)
- [ ] Document recovery procedures

### Maintenance
- [ ] RDS maintenance window configured
- [ ] RDS minor version auto-upgrade enabled/disabled
- [ ] ECS task definition versions managed
- [ ] ECR image cleanup policy verified

## 📝 Rollback Procedures

### If Deployment Fails
1. [ ] Check CloudWatch logs
2. [ ] Review CloudFormation events
3. [ ] Use AWS Console "Rollback" feature
4. [ ] Or delete and recreate stack

### If Application Issues
1. [ ] ECS console → Roll back to previous task definition
2. [ ] Or redeploy previous Docker image from ECR
3. [ ] Check database connection issues
4. [ ] Verify environment variables

## ✅ Final Checklist

- [ ] All stacks created successfully
- [ ] Database accessible from ECS
- [ ] Application responding to requests
- [ ] Pipeline triggers on Git push
- [ ] Logs visible in CloudWatch
- [ ] Costs within budget
- [ ] Team notified of new infrastructure
- [ ] Documentation updated
- [ ] Monitoring configured
- [ ] Backup strategy verified

## 🎓 Total Time Estimate

**First-Time Deployment:**
- Phase 1 (Database): ~35 minutes
- Phase 2 (Pipeline): ~25 minutes
- Phase 3 (Verification): ~12 minutes
- **Total: ~72 minutes (1 hour 12 minutes)**

**Subsequent Updates:**
- Via pipeline: ~5-8 minutes (automatic)

---

## 📚 Reference Documents

- **Quick Start**: `aws/QUICKSTART.md`
- **Full Plan**: `AWS_DEPLOYMENT_PLAN.md`
- **Workflow**: `aws/DEPLOYMENT_WORKFLOW.md`
- **README**: `aws/README.md`

---

**Good luck with your deployment! 🚀**

*Last updated: 2024*
