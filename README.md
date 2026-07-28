# Petclinic Migration Demo - OpenShift 4.14 to 4.20

A complete Spring Boot Petclinic application for demonstrating **OpenShift cluster migration** using **MTC (Migration Toolkit for Containers)** and **OADP (OpenShift API for Data Protection)**.

## Overview

This project demonstrates:
- ✅ Spring Boot 3.1.5 application deployment on OpenShift
- ✅ PostgreSQL database with persistent storage
- ✅ Redis cache deployment
- ✅ OADP backup and restore workflow
- ✅ MTC cluster migration (4.14 → 4.20)
- ✅ Data persistence across restarts and migrations
- ✅ Auto-seeding with 50+ records (10 owners, 12 pets, 6 vets, 12 visits)

## Project Structure

```
.
├── Dockerfile-OpenShift          # Container image definition (Red Hat UBI8)
├── README.md                      # This file
├── pom.xml                        # Maven build configuration
├── k8s/
│   └── petclinic-complete.yaml   # Complete Kubernetes deployment (all resources)
├── oadp/
│   ├── source-dpa.yaml           # OADP DataProtectionApplication (source cluster)
│   ├── target-dpa.yaml           # OADP DataProtectionApplication (target cluster)
│   ├── migration-backup.yaml     # Backup resource definition
│   ├── target-restore.yaml       # Restore resource definition
│   ├── bucket-credentials.yaml   # S3 bucket credentials (template)
│   └── target-storage-mapping.yaml # Storage class mapping for migration
└── src/main/
    ├── java/org/acme/petclinic/
    │   ├── DataInitializer.java  # Auto-seed data on first deployment
    │   ├── PetclinicApplication.java
    │   ├── entities/             # JPA entities
    │   ├── repositories/         # Spring Data JPA repositories
    │   └── resources/            # REST controllers
    └── resources/
        ├── application.properties # Spring Boot configuration
        └── static/index.html      # UI homepage
```

## Quick Start

### Prerequisites
- OpenShift 4.14+ cluster access
- `oc` CLI installed
- Docker/Podman installed (for building images)
- Maven 3.9+ (for building application)

### 1. Deploy on OpenShift (Source or Target Cluster)

```bash
# Login to cluster
oc login --token=YOUR_TOKEN --server=YOUR_SERVER

# Apply all resources (namespace, PostgreSQL, Redis, App, RBAC)
oc apply -f k8s/petclinic-complete.yaml

# Wait for deployment
oc rollout status deployment/petclinic-app -n petclinic-demo

# Get application URL
oc get route -n petclinic-demo petclinic-route -o jsonpath='{.spec.host}'

# Verify data seeded
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"
```

### 2. Build and Push Docker Image

```bash
# Build for AMD64 (OpenShift is x86_64)
podman build --platform linux/amd64 -f Dockerfile-OpenShift -t petclinic:v1 .

# Push to registry
podman tag petclinic:v1 quay.io/YOUR_ORG/petclinic:v1
podman push quay.io/YOUR_ORG/petclinic:v1
```

### 3. Backup with OADP (Source Cluster)

```bash
# Ensure OADP is installed
oc get pods -n openshift-adp

# Create backup
oc create -f oadp/migration-backup.yaml

# Monitor backup
oc get backup -n openshift-adp migration-backup -w
oc describe backup -n openshift-adp migration-backup
```

### 4. Restore with OADP (Target Cluster)

```bash
# Login to target cluster (4.20)
oc login --server=TARGET_SERVER

# Apply DPA on target
oc apply -f oadp/target-dpa.yaml

# Create restore
oc apply -f oadp/target-restore.yaml

# Monitor restore
oc get restore -n openshift-adp target-restore -w
```

## Configuration

### Database
- **Engine:** PostgreSQL 16 (Red Hat UBI image)
- **Connection:** `jdbc:postgresql://postgres:5432/petclinic`
- **Credentials:** postgres/petclinicpwd
- **Storage:** 5Gi PVC (gp3-csi for AWS, Ceph RBD for on-prem)

### Application
- **Framework:** Spring Boot 3.1.5
- **Java:** OpenJDK 17
- **DDL Strategy:** `ddl-auto=update` (creates schema, preserves data)
- **Data Seeding:** Auto-seeds 50+ records on first deployment
- **Health Check:** `/actuator/health` (liveness + readiness)

### Storage Classes
- **Source (4.14 - AWS):** `gp3-csi`
- **Target (4.20 - On-Prem):** `ocs-external-storagecluster-ceph-rbd-immediate` (update in YAML)

## Data Model

### Owner (10 records)
- ID, First Name, Last Name, Address, City, Telephone

### Pet (12 records)
- ID, Name, Birth Date, Owner ID, Pet Type

### Vet (6 records)
- ID, First Name, Last Name, Specialty

### Visit (12 records)
- ID, Date, Description, Pet ID, Vet ID

### Pet Type (5 records)
- Dog, Cat, Bird, Rabbit, Hamster

## Key Features

### ✅ Data Persistence
```yaml
spring.jpa.hibernate.ddl-auto=update  # Never drops tables
```
- Creates schema on first startup
- Updates schema on subsequent runs
- **Preserves data across pod restarts and migrations**

### ✅ Auto-Seeding
```java
if (ownerRepository.count() == 0) {
    initializeData();  // Seeds only on first deployment
}
```
- Seeds 50+ records automatically
- Skips seeding on subsequent restarts
- No manual data loading needed

### ✅ Kubernetes Integration
- Labels: `app.kubernetes.io/name: petclinic` (for topology grouping)
- Security Context: `runAsNonRoot: true`
- RBAC: Minimal permissions for pod
- Anti-affinity: Spreads replicas across nodes

### ✅ Migration Ready
- No hardcoded hostnames (Route auto-generates)
- Configurable database URL
- Storage class agnostic (set in k8s-complete.yaml)
- OADP backup/restore workflow

## Troubleshooting

### Pod in CrashLoopBackOff
```bash
# Check logs
oc logs deployment/petclinic-app -n petclinic-demo

# Common issues:
# 1. Database doesn't exist
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -c "CREATE DATABASE petclinic;"

# 2. Tables don't exist (wrong ddl-auto setting)
# Update k8s/petclinic-complete.yaml and reapply

# 3. Can't connect to postgres
oc exec deployment/petclinic-app -n petclinic-demo -- nc -zv postgres 5432
```

### PVC Stuck in Pending
```bash
# Check storage classes
oc get storageclass

# Update k8s/petclinic-complete.yaml with correct storage class name
# For AWS: gp3-csi
# For On-Prem: ocs-external-storagecluster-ceph-rbd-immediate

# Reapply
oc apply -f k8s/petclinic-complete.yaml
```

### OADP Backup Fails
```bash
# Verify OADP is installed
oc get pods -n openshift-adp

# Check backup storage location
oc get backupstoragelocation -n openshift-adp

# View backup errors
oc describe backup -n openshift-adp migration-backup
```

## Deployment Variants

### Source Cluster (4.14)
```bash
# Use AWS storage class
oc apply -f k8s/petclinic-complete.yaml
```

### Target Cluster (4.20)
```bash
# Edit k8s/petclinic-complete.yaml
# Change: storageClassName: gp3-csi
# To: storageClassName: ocs-external-storagecluster-ceph-rbd-immediate

# Then deploy
oc apply -f k8s/petclinic-complete.yaml
```

## API Endpoints

Once deployed, access via the Route:

- **List Owners:** `GET /owners`
- **Add Owner:** `POST /owners`
- **List Pets:** `GET /pets`
- **List Vets:** `GET /vets`
- **List Visits:** `GET /visits`
- **Health:** `GET /actuator/health`
- **Metrics:** `GET /actuator/metrics`

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | JVM memory settings |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/petclinic` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `petclinicpwd` | DB password |

## Migration Workflow

### Step 1: Backup on Source (4.14)
```bash
oc apply -f oadp/source-dpa.yaml
oc create -f oadp/migration-backup.yaml
```

### Step 2: Switch to Target (4.20)
```bash
oc login --server=TARGET_SERVER
```

### Step 3: Restore on Target (4.20)
```bash
oc apply -f oadp/target-dpa.yaml
oc apply -f oadp/target-restore.yaml
```

### Step 4: Verify
```bash
oc get all -n petclinic-demo
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"
```

## Testing Data Persistence

```bash
# Check current count
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"

# Restart pods
oc delete pods -l app.kubernetes.io/name=petclinic -n petclinic-demo

# Check again - count should be SAME!
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"
```

## Topology View

In OpenShift Console → Topology:
- All 3 components (PostgreSQL, Redis, Petclinic App) grouped under **one application** `petclinic`
- Connections shown between services
- Resource status visible at a glance

## Performance

- **App Memory:** 256Mi (requests) / 512Mi (limits)
- **App CPU:** 250m (requests) / 500m (limits)
- **Startup Time:** ~30 seconds
- **Data Seeding Time:** ~5 seconds (first run only)

## Production Recommendations

1. ✅ Use `ddl-auto=update` (never use `create`)
2. ✅ Enable OADP backups (daily or after changes)
3. ✅ Use storage class appropriate to your infrastructure
4. ✅ Scale replicas to 2+ for HA
5. ✅ Monitor logs and metrics via Prometheus
6. ✅ Use Secrets for sensitive data (not ConfigMaps)
7. ✅ Enable network policies to restrict traffic

## License

MIT

## Support

For questions or issues, check:
- OpenShift documentation: https://docs.openshift.com/
- Spring Boot documentation: https://spring.io/projects/spring-boot
- OADP documentation: https://docs.openshift.com/container-platform/latest/backup_and_restore/index.html

## Migration Checklist

- [ ] Source cluster backup created and verified
- [ ] Target cluster prepared with OADP
- [ ] Storage class mapping configured
- [ ] Backup restored on target cluster
- [ ] All pods running on target
- [ ] Data verified (SELECT COUNT(*) matches)
- [ ] Application accessible via route
- [ ] Data persists after pod restart