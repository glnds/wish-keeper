# AWS Deployment Workflow - Visual Guide

## 🎯 Deployment Flow Overview

```
┌────────────────────────────────────────────────────────────────────────┐
│                    PHASE 1: Database Migration                          │
└────────────────────────────────────────────────────────────────────────┘

Step 1: Deploy VPC
┌─────────────┐
│   VPC.yml   │──▶ Creates VPC with public/private subnets
└─────────────┘    ├─ 2 Public subnets (ALB)
                   ├─ 2 Private subnets (ECS)
                   ├─ 2 Private subnets (RDS)
                   ├─ NAT Gateways
                   └─ Internet Gateway

Step 2: Deploy RDS
┌─────────────┐
│   RDS.yml   │──▶ Creates PostgreSQL database
└─────────────┘    ├─ PostgreSQL 15.5
                   ├─ Encrypted at rest
                   ├─ In private subnets
                   ├─ Security groups
                   ├─ Automated backups
                   └─ CloudWatch monitoring

Step 3: Migrate Schema
┌──────────────────┐
│ migrate-to-rds.sh│──▶ Applies database schema
└──────────────────┘    ├─ Reads: src/main/resources/People.pgsql
                        ├─ Reads: src/main/resources/wishes.pgsql
                        ├─ Connects to RDS
                        ├─ Creates tables
                        └─ Verifies success

Result: ✅ Database ready in AWS RDS


┌────────────────────────────────────────────────────────────────────────┐
│                  PHASE 2: CI/CD Pipeline Setup                          │
└────────────────────────────────────────────────────────────────────────┘

Step 4: Create ECR Repository
┌─────────────┐
│   ECR.yml   │──▶ Docker image repository
└─────────────┘    ├─ Image scanning enabled
                   ├─ Lifecycle policies
                   └─ Encryption enabled

Step 5: Connect GitHub
┌──────────────────────┐
│ GitHub Connection    │──▶ AWS CodeStar connection
└──────────────────────┘    ├─ OAuth/App authentication
                            └─ Webhook for push events

Step 6: Deploy CodePipeline
┌──────────────────┐
│ CodePipeline.yml │──▶ Creates CI/CD pipeline
└──────────────────┘    ├─ Source stage (GitHub)
                        ├─ Build stage (CodeBuild)
                        └─ Deploy stage (ECS)

Result: ✅ Automated CI/CD pipeline


┌────────────────────────────────────────────────────────────────────────┐
│              PHASE 3: Application Deployment                            │
└────────────────────────────────────────────────────────────────────────┘

Step 7: Deploy ECS Cluster
┌─────────────────┐
│ ECS Cluster     │──▶ Container orchestration
└─────────────────┘    ├─ Fargate launch type
                       └─ CloudWatch logs

Step 8: Create ECS Service
┌─────────────────┐
│ ECS Service     │──▶ Runs application containers
└─────────────────┘    ├─ Task definition
                       ├─ Environment variables
                       ├─ Secrets from Secrets Manager
                       └─ Health checks

Step 9: Add Load Balancer (Optional)
┌─────────────────┐
│      ALB        │──▶ Distributes traffic
└─────────────────┘    ├─ Public facing
                       ├─ Health checks: /api/hello
                       └─ Routes to ECS tasks

Result: ✅ Application running in AWS
```

## 🔄 CI/CD Pipeline Flow

```
Developer Pushes Code
         │
         ▼
┌────────────────────┐
│   GitHub Webhook   │ Triggers pipeline
└────────────────────┘
         │
         ▼
┌────────────────────┐
│  Source Stage      │
│  (CodePipeline)    │ Fetches code from GitHub
└────────────────────┘
         │
         ▼
┌────────────────────┐
│  Build Stage       │
│  (CodeBuild)       │
└────────────────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌─────────┐ ┌────────────┐
│  Maven  │ │  Docker    │
│  Build  │ │  Build     │
└─────────┘ └────────────┘
    │         │
    └────┬────┘
         ▼
┌────────────────────┐
│  Push to ECR       │ Stores Docker image
└────────────────────┘
         │
         ▼
┌────────────────────┐
│  Deploy Stage      │
│  (ECS)             │ Updates service
└────────────────────┘
         │
         ▼
┌────────────────────┐
│  New Task Starts   │ Blue/Green deployment
└────────────────────┘
         │
    ┌────┴────┐
    │  Health │ Checks pass?
    │  Check  │
    └────┬────┘
         │
    ┌────┴────┐
    │   Yes   │   No
    ▼         ▼
┌────────┐ ┌──────────┐
│Traffic │ │ Rollback │
│Routed  │ │          │
└────────┘ └──────────┘
```

## 📊 Data Flow Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Internet                                  │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
                  ┌────────────────┐
                  │ Application    │
                  │ Load Balancer  │ (Public Subnet)
                  └────────┬───────┘
                           │
           ┌───────────────┼───────────────┐
           │                               │
           ▼                               ▼
   ┌──────────────┐              ┌──────────────┐
   │  ECS Task 1  │              │  ECS Task 2  │ (Private Subnet)
   │  Java App    │              │  Java App    │
   │  Port 8000   │              │  Port 8000   │
   └──────┬───────┘              └──────┬───────┘
          │                              │
          └──────────────┬───────────────┘
                         │
                         ▼
                ┌────────────────┐
                │  RDS PostgreSQL│ (Private Subnet)
                │  Port 5432     │
                └────────────────┘
```

## 🔐 Security Flow

```
Authentication & Authorization
┌────────────────────────────────────────────────────────┐
│  1. GitHub Connection                                  │
│     - CodeStar connection with OAuth                   │
│     - Webhook authentication                           │
│                                                        │
│  2. AWS IAM Roles                                      │
│     - CodeBuild Service Role                           │
│     - CodePipeline Service Role                        │
│     - ECS Task Execution Role                          │
│     - ECS Task Role                                    │
│                                                        │
│  3. Secrets Management                                 │
│     - DB credentials in Secrets Manager                │
│     - ECS tasks fetch secrets at runtime               │
│     - No secrets in code or environment variables      │
│                                                        │
│  4. Network Security                                   │
│     - Security Groups (stateful firewall)              │
│       ├─ ALB: Allow 80/443 from internet              │
│       ├─ ECS: Allow 8000 from ALB only                │
│       └─ RDS: Allow 5432 from ECS only                │
│     - Private Subnets for ECS and RDS                  │
│     - NAT Gateways for outbound only                   │
│                                                        │
│  5. Data Security                                      │
│     - RDS encryption at rest (AES-256)                 │
│     - SSL/TLS for database connections                 │
│     - ECR image scanning                               │
└────────────────────────────────────────────────────────┘
```

## 📈 Monitoring & Logging Flow

```
Application Metrics & Logs
┌────────────────────────────────────────────────────────┐
│                                                        │
│  ECS Task Logs ──────────▶ CloudWatch Logs            │
│  (/ecs/wish-keeper)                                   │
│                                   │                    │
│  CodeBuild Logs ─────────▶       │                    │
│  (/aws/codebuild/...)             │                    │
│                                   │                    │
│  RDS Logs ───────────────▶        │                    │
│  (Slow queries, errors)           │                    │
│                                   ▼                    │
│                          ┌────────────────┐            │
│                          │  CloudWatch    │            │
│                          │  Metrics       │            │
│                          └────────┬───────┘            │
│                                   │                    │
│  Application Metrics              │                    │
│  - ECS CPU/Memory ────────────────┤                    │
│  - RDS CPU/Connections ───────────┤                    │
│  - ALB Request Count ─────────────┤                    │
│                                   │                    │
│                                   ▼                    │
│                          ┌────────────────┐            │
│                          │  CloudWatch    │            │
│                          │  Alarms        │            │
│                          └────────┬───────┘            │
│                                   │                    │
│                                   ▼                    │
│                          ┌────────────────┐            │
│                          │  SNS Topic     │            │
│                          │  (Notifications)│           │
│                          └────────────────┘            │
│                                                        │
└────────────────────────────────────────────────────────┘
```

## ⚡ Build Process Details

```
buildspec.yml Execution Flow
┌────────────────────────────────────────────────────────┐
│                                                        │
│  1. PRE_BUILD Phase                                    │
│     ┌────────────────────────────────────┐            │
│     │ • Login to ECR                     │            │
│     │ • Set environment variables        │            │
│     │ • Determine image tag (commit SHA) │            │
│     └────────────────────────────────────┘            │
│                     │                                  │
│                     ▼                                  │
│  2. BUILD Phase                                        │
│     ┌────────────────────────────────────┐            │
│     │ • Run mvn clean package            │            │
│     │   - Downloads dependencies         │            │
│     │   - Compiles Java code             │            │
│     │   - Creates fat JAR (app.jar)      │            │
│     │ • Build Docker image               │            │
│     │   - Multi-stage build              │            │
│     │   - Optimized layers               │            │
│     │ • Tag image (latest + commit SHA)  │            │
│     └────────────────────────────────────┘            │
│                     │                                  │
│                     ▼                                  │
│  3. POST_BUILD Phase                                   │
│     ┌────────────────────────────────────┐            │
│     │ • Push images to ECR               │            │
│     │   - latest tag                     │            │
│     │   - commit SHA tag                 │            │
│     │ • Create imagedefinitions.json     │            │
│     │ • Create taskdef.json              │            │
│     │   - Task definition with secrets   │            │
│     │   - Environment variables          │            │
│     │   - Health check configuration     │            │
│     └────────────────────────────────────┘            │
│                     │                                  │
│                     ▼                                  │
│  4. ARTIFACTS                                          │
│     ┌────────────────────────────────────┐            │
│     │ • imagedefinitions.json            │            │
│     │ • taskdef.json                     │            │
│     └────────────────────────────────────┘            │
│                     │                                  │
│                     ▼                                  │
│              Deploy to ECS                             │
│                                                        │
└────────────────────────────────────────────────────────┘
```

## 🚀 Deployment Timeline

```
┌─────────────────────────────────────────────────────────────┐
│  First-Time Deployment (Total: ~45-60 minutes)             │
└─────────────────────────────────────────────────────────────┘

Phase 1: Infrastructure (35-40 min)
├─ VPC deployment           ────────────────  10 min
├─ RDS deployment           ──────────────────────────  20 min
└─ Schema migration         ────  2 min

Phase 2: CI/CD Setup (5-10 min)
├─ ECR repository           ─  1 min
├─ GitHub connection        ──  3 min
└─ CodePipeline deployment  ────  5 min

Phase 3: Application (5-10 min)
├─ Initial image build      ────  5 min
├─ ECS cluster              ─  1 min
├─ ECS service              ──  3 min
└─ Health checks            ─  1 min

┌─────────────────────────────────────────────────────────────┐
│  Subsequent Deployments (Total: ~5-8 minutes)              │
└─────────────────────────────────────────────────────────────┘

Git Push ▶ Pipeline Trigger ─  1 sec
         ▶ Build Phase      ────  3-4 min
         ▶ Deploy Phase     ──  2-3 min
         ▶ Health Checks    ─  30 sec
         ▶ Traffic Switch   ─  1 sec
```

## 📋 Verification Checklist

```
After Each Phase
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Phase 1: Database                                          │
│  ☐ VPC created with correct CIDR                           │
│  ☐ 2 public + 2 private subnets in different AZs           │
│  ☐ RDS instance is "available" status                      │
│  ☐ RDS endpoint is accessible                              │
│  ☐ Schema migration completed successfully                 │
│  ☐ Tables exist: SELECT COUNT(*) FROM people;              │
│                                                             │
│  Phase 2: CI/CD                                             │
│  ☐ ECR repository created                                  │
│  ☐ GitHub connection is "AVAILABLE" status                 │
│  ☐ CodeBuild project exists                                │
│  ☐ CodePipeline created                                    │
│  ☐ Pipeline can fetch code from GitHub                     │
│                                                             │
│  Phase 3: Application                                       │
│  ☐ ECS cluster is active                                   │
│  ☐ ECS service is running                                  │
│  ☐ Tasks are in RUNNING state                              │
│  ☐ Health checks passing                                   │
│  ☐ Application accessible                                  │
│  ☐ Database connection working                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## 🔍 Troubleshooting Decision Tree

```
Deployment Failed?
        │
        ├─ CloudFormation Stack?
        │  ├─ Check Events tab in console
        │  ├─ Look for first CREATE_FAILED
        │  └─ Fix parameter or resource issue
        │
        ├─ CodeBuild Failed?
        │  ├─ Check build logs
        │  ├─ Maven errors? → Check pom.xml
        │  ├─ Docker errors? → Check Dockerfile
        │  └─ ECR errors? → Check IAM permissions
        │
        ├─ ECS Tasks Not Starting?
        │  ├─ Check task logs in CloudWatch
        │  ├─ Container errors? → App configuration
        │  ├─ Can't pull image? → ECR permissions
        │  └─ Database errors? → Check DB_HOST
        │
        └─ Application Not Working?
           ├─ Check /api/hello endpoint
           ├─ Database connection? → Security groups
           ├─ 500 errors? → Check CloudWatch logs
           └─ Timeout? → Check health check config
```

---

**Ready to deploy?** Start with [QUICKSTART.md](QUICKSTART.md) 🚀
