# 🐾 Petclinic - OpenShift Deployment Guide

## Quick Start - Deploy to OpenShift in 3 Steps

### Prerequisites
- OpenShift cluster (4.16+) access
- `oc` CLI configured and logged in
- Application source code cloned from Git
- Docker image already pushed to registry: `quay.io/nehgupta/petclinic:1.0.0`

---

## Step 1: Clone Application from Git

```bash
git clone https://github.com/YOUR_USERNAME/petclinic.git
cd petclinic
```

The application already includes:
- ✅ Spring Boot source code
- ✅ All Kubernetes manifests (k8s/ directory)
- ✅ Docker image (pre-built and pushed)

---

## Step 2: Deploy All Resources to OpenShift

```bash
# Login to OpenShift
oc login --token=YOUR_TOKEN --server=YOUR_SERVER

# Deploy all Kubernetes manifests
oc apply -f k8s/

# This creates:
# - Namespace: petclinic-demo
# - Storage: PersistentVolumeClaims
# - Database: PostgreSQL 16
# - Application: Spring Boot (2 replicas)
# - Networking: Service & Route
# - Security: ServiceAccount + RBAC
```

### Wait for PostgreSQL to be Ready

```bash
# Wait for PostgreSQL to be ready (2-3 minutes)
oc rollout status deployment/postgres -n petclinic-demo

# Verify postgres pod is running
oc get pods -n petclinic-demo | grep postgres
```

---

## Step 2.5: Create Database in PostgreSQL ⭐ IMPORTANT

PostgreSQL is running but the **petclinic database needs to be created manually**.

```bash
# Create the petclinic database
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -c "CREATE DATABASE petclinic;"

# Verify database was created
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -c "\l"

# Expected output: petclinic database should appear in the list
```

**Note:** The ConfigMap tries to create it, but you need to do it manually if it wasn't created. The tables will be auto-created when the Spring Boot application starts.

---

## Step 3: Wait for Application Deployment

```bash
# Wait for Application to be ready
oc rollout status deployment/petclinic-app -n petclinic-demo

# This will:
# 1. Start the Spring Boot application
# 2. Connect to PostgreSQL
# 3. Auto-create tables (via Hibernate)
# 4. Seed 50+ records (via DataInitializer)
```

---

## Step 4: Access Application

### Get Application URL
```bash
oc get route petclinic-route -n petclinic-demo -o jsonpath='{.spec.host}'
```

### Open in Browser
```bash
# Copy the URL from above and paste in browser:
http://petclinic-route-petclinic-demo.apps.YOUR_CLUSTER_DOMAIN
```

---

## Database Setup Details

### What Gets Created Automatically
- **PostgreSQL 16 pod** - from Red Hat UBI image
- **5Gi persistent volume** - for data storage
- **ConfigMap** - with POSTGRES_DB=petclinic and POSTGRES_USER=postgres
- **Secret** - with POSTGRES_PASSWORD=petclinicpwd

### What You Need to Do Manually

**Create Database (Step 2.5):**
```bash
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -c "CREATE DATABASE petclinic;"
```

**Why manually?** - The environment variable `POSTGRES_DB` in some PostgreSQL setups doesn't auto-create the database. It's safer to create it explicitly.

### What Gets Created by Spring Boot App (Automatic)

Once the app starts, it will automatically:
1. **Connect** to PostgreSQL using credentials from ConfigMap/Secret
2. **Create tables** via Hibernate (JPA)
3. **Seed data** via DataInitializer class (50+ records):
   - 10 Pet Owners
   - 12 Pets
   - 6 Veterinarians
   - 12 Visits
   - 5 Pet Types

### Database Credentials (from k8s-postgres.yaml)

| Item | Value |
|------|-------|
| **Host** | postgres (internal Kubernetes service) |
| **Port** | 5432 |
| **Database** | petclinic |
| **Username** | postgres |
| **Password** | petclinicpwd |
| **Storage** | gp3-csi (5Gi persistent volume) |

---

## Verification Checklist

### ✅ Check All Pods Running
```bash
oc get pods -n petclinic-demo

# Expected output:
# postgres-xxxxx                  Running (1/1)
# petclinic-app-xxxxx-1          Running (1/1)
# petclinic-app-xxxxx-2          Running (1/1)
```

### ✅ Check Data Seeded in Database
```bash
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"

# Expected: 10 owners
```

### ✅ Test REST API
```bash
ROUTE=$(oc get route petclinic-route -n petclinic-demo -o jsonpath='{.spec.host}')
curl http://$ROUTE/owners | jq

# Expected: JSON array with 10 owners
```

### ✅ Check Application is Running
```bash
oc logs deployment/petclinic-app -n petclinic-demo --tail=10

# Look for: "Started PetclinicApplication"
```

---

## What Gets Created

### Namespace
- **petclinic-demo** - Isolated environment for all resources

### Storage
- **postgres-pvc** - 5Gi persistent volume for PostgreSQL data (gp3-csi)
- **files-pvc** - 1Gi persistent volume for application files

### Database
- **PostgreSQL 16** - Red Hat UBI certified image
- **Database Name:** petclinic
- **Username:** postgres
- **Password:** petclinicpwd (from Kubernetes secret)
- **Port:** 5432 (internal)
- **Auto-seeded Data:**
  - 10 Pet Owners
  - 12 Pets
  - 6 Veterinarians
  - 12 Visits

### Application
- **Framework:** Spring Boot 3.1.5
- **Language:** Java 17
- **Container Image:** quay.io/nehgupta/petclinic:1.0.0 (Red Hat UBI 8)
- **Replicas:** 2 (for high availability)
- **Port:** 8080
- **Memory Request:** 256Mi | Limit: 512Mi
- **CPU Request:** 250m | Limit: 500m

### Networking
- **Service:** petclinic-service (ClusterIP) - internal communication
- **Route:** petclinic-route (OpenShift Route) - external access
- **Hostname:** Auto-generated by OpenShift (no hardcoding = works in any cluster)

### Security
- **ServiceAccount:** petclinic-app
- **Role:** petclinic-app-role (access to ConfigMap/Secret)
- **RoleBinding:** petclinic-app-rolebinding
- **Security Context:** Non-root user, dropped all capabilities

---

## Making Changes to Application

### If You Need to Update Application Code

1. **Make changes** in your local repo
2. **Commit and push** to Git:
   ```bash
   git add .
   git commit -m "Your changes"
   git push origin main
   ```

3. **Rebuild Docker image** (only if code changed):
   ```bash
   podman build --platform linux/amd64 -f Dockerfile-OpenShift -t petclinic:1.0.0 .
   podman tag petclinic:1.0.0 quay.io/nehgupta/petclinic:1.0.0
   podman push quay.io/nehgupta/petclinic:1.0.0
   ```

4. **Restart deployment** in OpenShift:
   ```bash
   oc rollout restart deployment/petclinic-app -n petclinic-demo
   
   # Wait for new pods with updated image
   oc rollout status deployment/petclinic-app -n petclinic-demo
   ```

### If You Only Updated Configuration (No Code Changes)

```bash
# Just update the ConfigMap in cluster
oc apply -f k8s/k8s-app.yaml

# Restart app pods to pick up new config
oc rollout restart deployment/petclinic-app -n petclinic-demo
```

### If You Only Updated Kubernetes Manifests

```bash
# Reapply updated manifests
oc apply -f k8s/

# Verify changes
oc get all -n petclinic-demo
```

---

## Troubleshooting

### Problem: Pod in CrashLoopBackOff
```bash
# Check application logs
oc logs deployment/petclinic-app -n petclinic-demo --tail=50

# Common causes:
# 1. Database not ready yet → wait 2-3 minutes
# 2. Database connection error → check postgres logs
# 3. Image not found → verify image was pushed to registry
```

### Problem: Can't Access Application (Connection Refused)
```bash
# Check if all pods are running
oc get pods -n petclinic-demo

# Check if pods are ready (should show 1/1)
oc describe pod <pod-name> -n petclinic-demo

# Check service is created
oc get svc petclinic-service -n petclinic-demo

# Check route is created
oc get route petclinic-route -n petclinic-demo
```

### Problem: Database Connection Error
```bash
# Check PostgreSQL pod status
oc get pods -n petclinic-demo | grep postgres

# Check PostgreSQL logs
oc logs deployment/postgres -n petclinic-demo --tail=50

# Verify database was created
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -c "\l"

# Verify tables exist
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "\dt"
```

### Problem: No Data Visible in Application
```bash
# Check if data was seeded
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"

# If count is 0, data initializer runs on app startup
# Wait 1-2 minutes and refresh browser

# Check app logs for initialization message
oc logs deployment/petclinic-app -n petclinic-demo | grep -i "initializ"
```

---

## Key Files in Repository

```
petclinic/
├── README.md                      # This deployment guide
├── pom.xml                        # Maven dependencies
├── Dockerfile-OpenShift           # Multi-stage Docker build
│
├── src/
│   └── main/
│       ├── java/org/acme/petclinic/
│       │   ├── PetclinicApplication.java
│       │   ├── DataInitializer.java
│       │   ├── entities/
│       │   ├── repositories/
│       │   └── resources/
│       └── resources/
│           ├── application.properties
│           └── static/index.html
│
└── k8s/
    ├── k8s-namespace.yaml
    ├── k8s-pvc.yaml
    ├── k8s-postgres.yaml
    ├── k8s-app.yaml
    ├── k8s-serviceaccount.yaml
    └── k8s-route.yaml
```

---

## Cleanup (Remove from OpenShift)

```bash
# Delete entire namespace
oc delete namespace petclinic-demo
```

---

## Ready for Migration Demo

- ✅ **Stateless Migration (MTC)** - Application pods
- ✅ **Stateful Migration (OADP)** - Database with data
- ✅ **Multi-cluster Migration** - Auto-generated hostnames

---

**Version:** 1.0.0 | **OpenShift:** 4.16+ | **Spring Boot:** 3.1.5 | **PostgreSQL:** 16
