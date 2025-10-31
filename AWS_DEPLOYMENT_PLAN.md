# AWS Deployment Plan for Wish Keeper

## Overview
This plan deploys the Wish Keeper Java application to AWS with:
- **Amazon RDS PostgreSQL** for the database
- **Amazon ECS/Fargate** for container orchestration
- **AWS CodePipeline** for CI/CD automation
- **Amazon ECR** for Docker image storage
- **Application Load Balancer** for traffic distribution

## Phase 1: Database Migration to RDS (FIRST STEP)

### 1.1 Database Schema Extraction ✅
**Status**: Schema files already exist
- `src/main/resources/People.pgsql` - People table schema
- `src/main/resources/wishes.pgsql` - Wishes table schema

**Schema includes**:
- Custom ENUM type: `behavior_enum`
- Tables: `people`, `wishes`
- Foreign key constraints
- Default values

### 1.2 Create RDS PostgreSQL Instance

**CloudFormation Template**: `aws/cloudformation/rds.yml`

**Configuration**:
- Engine: PostgreSQL 15.x
- Instance Class: `db.t3.micro` (dev) or `db.t3.medium` (prod)
- Multi-AZ: Enabled (production)
- Storage: 20GB GP3, with autoscaling up to 100GB
- Backup: 7-day retention
- Security: VPC with security groups

**Environment Variables to Update**:
```bash
DB_HOST=<rds-endpoint>.rds.amazonaws.com
DB_PORT=5432
DB_NAME=webapp_db
DB_USER=wishkeeper
DB_PASSWORD=<stored-in-secrets-manager>
```

### 1.3 Database Migration Script

**Location**: `aws/scripts/migrate-to-rds.sh`

**Steps**:
1. Create RDS instance
2. Apply schema files to RDS
3. (Optional) Migrate existing data from local PostgreSQL
4. Validate schema and data

## Phase 2: CI/CD Pipeline with CodePipeline

### 2.1 Pipeline Architecture

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Source     │───▶│    Build     │───▶│    Test      │───▶│   Deploy     │
│  (GitHub)    │    │ (CodeBuild)  │    │ (CodeBuild)  │    │   (ECS)      │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### 2.2 Pipeline Components

#### Source Stage
- **Repository**: GitHub (this repository)
- **Branch**: `main` or `develop`
- **Trigger**: Webhook on push
- **Connection**: GitHub App or OAuth token

#### Build Stage
- **Service**: AWS CodeBuild
- **Buildspec**: `buildspec.yml`
- **Actions**:
  1. Maven compile and package
  2. Build Docker image
  3. Tag with commit SHA
  4. Push to Amazon ECR
  5. Update task definition

#### Test Stage (Optional)
- **Service**: AWS CodeBuild
- **Buildspec**: `buildspec-test.yml`
- **Actions**:
  1. Run Maven tests
  2. Integration tests against RDS
  3. Security scanning

#### Deploy Stage
- **Service**: Amazon ECS
- **Deployment Type**: Blue/Green deployment
- **Actions**:
  1. Update ECS task definition
  2. Deploy new task revision
  3. Health checks
  4. Route traffic to new version

### 2.3 Required AWS Resources

**CloudFormation Templates**:
1. `aws/cloudformation/ecr.yml` - Docker image registry
2. `aws/cloudformation/codebuild.yml` - Build projects
3. `aws/cloudformation/codepipeline.yml` - CI/CD pipeline
4. `aws/cloudformation/ecs-cluster.yml` - Container orchestration
5. `aws/cloudformation/ecs-service.yml` - Application service
6. `aws/cloudformation/alb.yml` - Load balancer
7. `aws/cloudformation/vpc.yml` - Network infrastructure

## Phase 3: Application Infrastructure

### 3.1 Container Orchestration
- **Service**: Amazon ECS with Fargate
- **Task Definition**: 
  - CPU: 512 (0.5 vCPU)
  - Memory: 1024 MB
  - Port: 8000
  - Environment variables from Secrets Manager

### 3.2 Networking
- **VPC**: Custom VPC with public and private subnets
- **Subnets**: 2+ AZs for high availability
- **Security Groups**:
  - ALB: Allow HTTP/HTTPS from internet
  - ECS: Allow 8000 from ALB
  - RDS: Allow 5432 from ECS

### 3.3 Load Balancing
- **Service**: Application Load Balancer
- **Listeners**: HTTP (80) → Target Group (8000)
- **Health Check**: `/api/hello`
- **SSL**: Optional ACM certificate

### 3.4 Secrets Management
- **Service**: AWS Secrets Manager
- **Secrets**:
  - `wish-keeper/db-credentials` - Database password
  - `wish-keeper/app-config` - Other sensitive configs

## Phase 4: Monitoring and Logging

### 4.1 CloudWatch Logs
- Container logs from ECS tasks
- CodeBuild build logs
- RDS slow query logs

### 4.2 CloudWatch Metrics
- ECS CPU/Memory utilization
- RDS connections, CPU, storage
- ALB request count, latency, errors

### 4.3 Alarms
- RDS high CPU (>80%)
- ECS task failures
- ALB 5xx errors
- Pipeline failures

## Implementation Steps

### Step 1: Prepare AWS Environment
```bash
# Create AWS resources directory
mkdir -p aws/{cloudformation,scripts,policies}

# Configure AWS CLI
aws configure
```

### Step 2: Create RDS Database
```bash
# Deploy RDS stack
aws cloudformation create-stack \
  --stack-name wish-keeper-rds \
  --template-body file://aws/cloudformation/rds.yml \
  --parameters file://aws/cloudformation/rds-params.json

# Apply database schema
psql -h <rds-endpoint> -U wishkeeper -d webapp_db -f src/main/resources/People.pgsql
psql -h <rds-endpoint> -U wishkeeper -d webapp_db -f src/main/resources/wishes.pgsql
```

### Step 3: Create ECR Repository
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-ecr \
  --template-body file://aws/cloudformation/ecr.yml
```

### Step 4: Setup VPC and Networking
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-vpc \
  --template-body file://aws/cloudformation/vpc.yml
```

### Step 5: Create ECS Cluster
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-ecs-cluster \
  --template-body file://aws/cloudformation/ecs-cluster.yml
```

### Step 6: Deploy Application Load Balancer
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-alb \
  --template-body file://aws/cloudformation/alb.yml
```

### Step 7: Create CodeBuild Projects
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-codebuild \
  --template-body file://aws/cloudformation/codebuild.yml \
  --capabilities CAPABILITY_IAM
```

### Step 8: Create CodePipeline
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-pipeline \
  --template-body file://aws/cloudformation/codepipeline.yml \
  --capabilities CAPABILITY_IAM
```

### Step 9: Deploy ECS Service
```bash
aws cloudformation create-stack \
  --stack-name wish-keeper-ecs-service \
  --template-body file://aws/cloudformation/ecs-service.yml \
  --capabilities CAPABILITY_IAM
```

## Cost Estimation (Monthly, US East-1)

### Development Environment
- RDS db.t3.micro (20GB): ~$15
- ECS Fargate (0.5 vCPU, 1GB): ~$15
- ALB: ~$20
- NAT Gateway: ~$35
- Data Transfer: ~$5
- **Total: ~$90/month**

### Production Environment
- RDS db.t3.medium Multi-AZ (50GB): ~$120
- ECS Fargate (2 tasks, auto-scaling): ~$50
- ALB: ~$20
- NAT Gateway (2 AZs): ~$70
- CloudWatch Logs: ~$10
- Secrets Manager: ~$1
- **Total: ~$270/month**

## Security Considerations

1. **Database**:
   - No public access
   - Encryption at rest
   - SSL/TLS connections
   - Automated backups

2. **Application**:
   - Private subnets
   - Security group rules
   - IAM roles with least privilege
   - Secrets in Secrets Manager

3. **Network**:
   - VPC isolation
   - NACLs for subnet protection
   - WAF for ALB (optional)
   - VPC Flow Logs

## Rollback Strategy

1. **Database**: RDS snapshots before schema changes
2. **Application**: ECS Blue/Green deployment with automatic rollback
3. **Infrastructure**: CloudFormation rollback on failure
4. **Pipeline**: Manual approval gates for production

## Next Steps

After this plan is implemented:
1. Add CloudFront CDN for global distribution
2. Implement auto-scaling policies
3. Add AWS WAF for application protection
4. Setup Route53 for DNS management
5. Implement multi-region deployment
6. Add X-Ray for distributed tracing
