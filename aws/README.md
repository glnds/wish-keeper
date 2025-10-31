# AWS Deployment for Wish Keeper

This directory contains all AWS infrastructure-as-code and deployment resources for the Wish Keeper application.

## 📁 Directory Structure

```
aws/
├── cloudformation/          # CloudFormation templates
│   ├── vpc.yml             # VPC with public/private subnets
│   ├── rds.yml             # PostgreSQL RDS database
│   ├── ecr.yml             # Docker image repository
│   ├── codepipeline.yml    # CI/CD pipeline
│   └── rds-params.json.example  # Example parameters
├── scripts/                # Deployment scripts
│   └── migrate-to-rds.sh   # Database migration script
├── policies/               # IAM policies (future)
├── QUICKSTART.md          # Quick deployment guide
└── README.md              # This file
```

## 🎯 Deployment Overview

The deployment consists of three main phases:

### Phase 1: Database Migration to RDS ⭐ **START HERE**
1. Create VPC with public/private subnets
2. Deploy PostgreSQL RDS instance
3. Migrate schema from Docker to RDS
4. Verify database connectivity

### Phase 2: CI/CD Pipeline Setup
5. Create ECR repository for Docker images
6. Setup GitHub connection
7. Deploy CodeBuild project
8. Create CodePipeline

### Phase 3: Application Deployment
9. Create ECS cluster and service
10. Deploy application with auto-scaling
11. Add Application Load Balancer
12. Setup monitoring and alarms

## 🚀 Quick Start

See [QUICKSTART.md](QUICKSTART.md) for detailed step-by-step instructions.

**Fastest path to deployment:**

```bash
# 1. Clone repository
cd /path/to/wish-keeper

# 2. Deploy VPC
cd aws/cloudformation
aws cloudformation create-stack \
  --stack-name wish-keeper-vpc \
  --template-body file://vpc.yml

# 3. Deploy RDS (after VPC completes)
# Edit rds-params.json with your values
aws cloudformation create-stack \
  --stack-name wish-keeper-rds \
  --template-body file://rds.yml \
  --parameters file://rds-params.json

# 4. Apply database schema
cd ../scripts
./migrate-to-rds.sh wish-keeper-rds

# 5. Setup CI/CD pipeline (see QUICKSTART.md)
```

## 📋 Prerequisites

### Required Tools
- AWS CLI v2.x
- PostgreSQL client (`psql`)
- Docker (for local testing)
- jq (JSON processor)
- Maven 3.x

### AWS Account Requirements
- Valid AWS account with admin or PowerUser access
- AWS CLI configured with credentials
- Region: us-east-1 (or modify templates for your region)

### GitHub Requirements
- Repository hosted on GitHub
- AWS CodeStar connection to GitHub
- Webhook permissions

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         AWS Cloud                            │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    VPC (10.0.0.0/16)                    │ │
│  │                                                          │ │
│  │  ┌─────────────────┐         ┌─────────────────┐       │ │
│  │  │  Public Subnet  │         │  Public Subnet  │       │ │
│  │  │   AZ-1 (ALB)    │         │   AZ-2 (ALB)    │       │ │
│  │  └────────┬────────┘         └────────┬────────┘       │ │
│  │           │                           │                 │ │
│  │  ┌────────▼────────┐         ┌────────▼────────┐       │ │
│  │  │ Private Subnet  │         │ Private Subnet  │       │ │
│  │  │  AZ-1 (ECS)     │         │  AZ-2 (ECS)     │       │ │
│  │  │  ┌──────────┐   │         │  ┌──────────┐   │       │ │
│  │  │  │ECS Task  │   │         │  │ECS Task  │   │       │ │
│  │  │  │Java App  │   │         │  │Java App  │   │       │ │
│  │  │  └────┬─────┘   │         │  └────┬─────┘   │       │ │
│  │  │       │         │         │       │         │       │ │
│  │  └───────┼─────────┘         └───────┼─────────┘       │ │
│  │          │                           │                  │ │
│  │  ┌───────▼───────────────────────────▼─────────┐       │ │
│  │  │        RDS PostgreSQL (Multi-AZ)            │       │ │
│  │  │          Private Subnets Only               │       │ │
│  │  └────────────────────────────────────────────┘       │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    CI/CD Pipeline                       │ │
│  │                                                          │ │
│  │  GitHub ──▶ CodePipeline ──▶ CodeBuild ──▶ ECR ──▶ ECS │ │
│  └─────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

## 🔒 Security Features

### Network Security
- ✅ Private subnets for application and database
- ✅ NAT Gateways for outbound internet access
- ✅ Security groups with least privilege
- ✅ No public access to RDS

### Data Security
- ✅ RDS encryption at rest (AES-256)
- ✅ SSL/TLS for database connections
- ✅ Secrets Manager for credentials
- ✅ Automated backups with point-in-time recovery

### Application Security
- ✅ ECR image scanning
- ✅ ECS task isolation
- ✅ IAM roles with least privilege
- ✅ CloudWatch logging for audit trail

## 📊 Monitoring & Observability

### CloudWatch Logs
- ECS container logs: `/ecs/wish-keeper`
- CodeBuild logs: `/aws/codebuild/wish-keeper-pipeline`
- RDS logs: PostgreSQL slow queries and errors

### CloudWatch Metrics
- RDS: CPU, connections, storage, IOPS
- ECS: CPU, memory, task count
- ALB: Request count, latency, errors
- CodePipeline: Success/failure rates

### CloudWatch Alarms
- RDS high CPU (>80%)
- RDS low storage (<2GB)
- ECS task failures
- Pipeline failures

## 💰 Cost Breakdown

### Development Environment (~$90/month)
```
RDS db.t3.micro (20GB)       $15
ECS Fargate (0.5 vCPU)       $15
Application Load Balancer     $20
NAT Gateway (1)              $35
Data Transfer                 $5
──────────────────────────────
Total                        ~$90/month
```

### Production Environment (~$270/month)
```
RDS db.t3.medium Multi-AZ    $120
ECS Fargate (2 tasks)        $50
Application Load Balancer     $20
NAT Gateways (2)             $70
CloudWatch & Logs            $10
──────────────────────────────
Total                        ~$270/month
```

**Cost Optimization Tips:**
- Use single NAT Gateway for dev/staging
- Enable Fargate Spot for non-production
- Set appropriate log retention periods
- Use S3 lifecycle policies for artifacts
- Enable RDS storage autoscaling

## 🔄 CI/CD Pipeline Details

### Pipeline Stages

1. **Source Stage**
   - Trigger: Git push to main branch
   - Source: GitHub via CodeStar connection
   - Output: Source code ZIP

2. **Build Stage**
   - Maven compile and package
   - Docker image build
   - Push to Amazon ECR
   - Create task definition
   - Output: Image definitions

3. **Deploy Stage**
   - Update ECS service
   - Blue/Green deployment
   - Health checks
   - Automatic rollback on failure

### Build Process

The `buildspec.yml` defines:
- Pre-build: ECR login, environment setup
- Build: Maven package, Docker build
- Post-build: ECR push, task definition creation

### Environment Variables

Set in CodeBuild project:
- `AWS_DEFAULT_REGION` - AWS region
- `AWS_ACCOUNT_ID` - AWS account
- `IMAGE_REPO_NAME` - ECR repository
- `DB_HOST` - RDS endpoint
- `DB_PASSWORD_SECRET_ARN` - Secrets Manager ARN

## 🗄️ Database Management

### Schema Files
- `src/main/resources/People.pgsql` - People table
- `src/main/resources/wishes.pgsql` - Wishes table

### Migration Script
The `migrate-to-rds.sh` script:
1. Retrieves RDS endpoint from CloudFormation
2. Gets credentials from Secrets Manager
3. Applies schema files to RDS
4. Verifies table creation
5. (Optional) Migrates data from local Docker

### Manual Migration

```bash
# Get DB password
export DB_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id wish-keeper-rds-db-credentials \
  --query 'SecretString' --output text | jq -r '.password')

# Apply schema
PGPASSWORD=$DB_PASSWORD psql \
  -h <rds-endpoint> \
  -U wishkeeper \
  -d webapp_db \
  -f src/main/resources/People.pgsql

PGPASSWORD=$DB_PASSWORD psql \
  -h <rds-endpoint> \
  -U wishkeeper \
  -d webapp_db \
  -f src/main/resources/wishes.pgsql
```

## 🧪 Testing

### Test RDS Connection

```bash
# From local machine (requires VPN or bastion)
psql -h <rds-endpoint> -U wishkeeper -d webapp_db

# From ECS task (using ECS Exec)
aws ecs execute-command \
  --cluster wish-keeper-cluster \
  --task <task-id> \
  --container wish-keeper-app \
  --interactive \
  --command "/bin/bash"
```

### Test Application

```bash
# Health check
curl http://<alb-dns>/api/hello

# Create a person
curl -X POST http://<alb-dns>/api/people \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1990-01-01"
  }'
```

## 🆘 Troubleshooting

### Common Issues

**Pipeline fails at Build stage**
```bash
# Check CodeBuild logs
aws logs tail /aws/codebuild/wish-keeper-pipeline --follow

# Verify buildspec.yml exists in repository
```

**ECS tasks fail to start**
```bash
# Check task logs
aws logs tail /ecs/wish-keeper --follow

# Check stopped tasks
aws ecs describe-tasks \
  --cluster wish-keeper-cluster \
  --tasks <task-arn>
```

**Cannot connect to RDS**
```bash
# Verify security group rules
aws ec2 describe-security-groups \
  --group-ids <rds-sg-id>

# Check RDS status
aws rds describe-db-instances \
  --db-instance-identifier wish-keeper-rds-postgres
```

**Application crashes on startup**
```bash
# Check environment variables
aws ecs describe-task-definition \
  --task-definition wish-keeper-task

# Verify secret exists
aws secretsmanager describe-secret \
  --secret-id wish-keeper-rds-db-credentials
```

## 📚 Additional Resources

- [AWS Deployment Plan](../AWS_DEPLOYMENT_PLAN.md) - Complete deployment strategy
- [Quick Start Guide](QUICKSTART.md) - Step-by-step deployment
- [Main README](../README.md) - Application documentation
- [CLAUDE.md](../CLAUDE.md) - Development guidelines

## 🤝 Contributing

When making changes to infrastructure:

1. Test changes in dev environment first
2. Update CloudFormation templates
3. Document parameter changes
4. Update cost estimates
5. Test deployment from scratch
6. Update this README

## 📝 License

See main project LICENSE file.
