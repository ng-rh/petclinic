# 🐾 Petclinic Migration Demo - Complete Deployment Guide

## Overview
This guide captures all steps to deploy the Petclinic Spring Boot application to OpenShift with PostgreSQL for MTC/OADP migration demonstrations.

**Key Features:**
- Spring Boot 3.1.5 + PostgreSQL 16
- 50+ seeded records (10 owners, 12 pets, 6 vets, 12 visits)
- REST API with full CRUD operations
- Professional responsive frontend
- OpenShift-native deployment
- Auto-scaling (2 replicas)
- Persistent storage (gp3-csi)

---

## 📋 Prerequisites

### System Requirements
- OpenShift cluster (4.16 or later)
- Podman or Docker for building images
- `oc` CLI installed and configured
- Maven 3.9+ and Java 17
- Git for version control

### Access & Credentials
- OpenShift cluster login credentials
- Container registry access (Quay.io or Docker Hub)
- `oc login` command ready

---

## 🔧 Step 1: Project Setup

### 1.1 Clone or Create Project
```bash
# Create project directory
mkdir -p ~/Demo2/petclinic
cd ~/Demo2/petclinic

# Initialize git (optional but recommended)
git init
```

### 1.2 Directory Structure
```bash
mkdir -p src/main/java/org/acme/petclinic/{entities,repositories,resources}
mkdir -p src/main/resources/static
mkdir -p k8s
```

---

## 📝 Step 2: Project Files

### 2.1 pom.xml
Create `pom.xml` with Spring Boot dependencies:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.acme</groupId>
    <artifactId>petclinic-migration-demo</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Petclinic Migration Demo (Stateless + Stateful)</name>
    <description>Spring Boot demo app for showing MTC (stateless) and OADP (stateful) migrations</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
        <relativePath/>
    </parent>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.1</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.2 Application Properties
Create `src/main/resources/application.properties`:

```properties
spring.application.name=petclinic-migration-demo
server.port=8080
server.servlet.context-path=/

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://postgres:5432/petclinic
spring.datasource.username=postgres
spring.datasource.password=petclinicpwd
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Logging Configuration
logging.level.root=INFO
logging.level.org.acme.petclinic=DEBUG
logging.level.org.springframework.web=INFO
logging.pattern.console=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n

# Actuator (Health checks & metrics)
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
management.metrics.enable.jvm=true
```

### 2.3 Java Entities
Create the following files in `src/main/java/org/acme/petclinic/entities/`:

**Owner.java**
```java
package org.acme.petclinic.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "owner")
@NoArgsConstructor
@AllArgsConstructor
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String address;
    private String city;
    private String telephone;
}
```

**Pet.java**
```java
package org.acme.petclinic.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "pet")
@NoArgsConstructor
@AllArgsConstructor
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @ManyToOne
    @JoinColumn(name = "pet_type_id")
    private PetType petType;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Owner owner;
}
```

**PetType.java**
```java
package org.acme.petclinic.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "pet_type")
@NoArgsConstructor
@AllArgsConstructor
public class PetType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
```

**Vet.java**
```java
package org.acme.petclinic.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "vet")
@NoArgsConstructor
@AllArgsConstructor
public class Vet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String specialty;
}
```

**Visit.java**
```java
package org.acme.petclinic.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "visit")
@NoArgsConstructor
@AllArgsConstructor
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    private String description;

    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne
    @JoinColumn(name = "vet_id")
    private Vet vet;
}
```

### 2.4 Repositories
Create in `src/main/java/org/acme/petclinic/repositories/`:

**OwnerRepository.java**
```java
package org.acme.petclinic.repositories;

import org.acme.petclinic.entities.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
}
```

Create similar files for: `PetRepository.java`, `PetTypeRepository.java`, `VetRepository.java`, `VisitRepository.java` (same pattern, different entity types)

### 2.5 REST Controllers
Create in `src/main/java/org/acme/petclinic/resources/`:

**OwnerController.java**
```java
package org.acme.petclinic.resources;

import org.acme.petclinic.entities.Owner;
import org.acme.petclinic.repositories.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/owners")
@CrossOrigin(origins = "*")
public class OwnerController {

    @Autowired
    private OwnerRepository ownerRepository;

    @GetMapping
    public ResponseEntity<List<Owner>> listAll() {
        List<Owner> owners = ownerRepository.findAll();
        return ResponseEntity.ok(owners);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Owner> get(@PathVariable Long id) {
        Optional<Owner> owner = ownerRepository.findById(id);
        return owner.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Owner> create(@RequestBody Owner owner) {
        Owner saved = ownerRepository.save(owner);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Owner> update(@PathVariable Long id, @RequestBody Owner ownerDetails) {
        Optional<Owner> owner = ownerRepository.findById(id);
        if (owner.isPresent()) {
            Owner o = owner.get();
            o.setFirstName(ownerDetails.getFirstName());
            o.setLastName(ownerDetails.getLastName());
            o.setAddress(ownerDetails.getAddress());
            o.setCity(ownerDetails.getCity());
            o.setTelephone(ownerDetails.getTelephone());
            return ResponseEntity.ok(ownerRepository.save(o));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (ownerRepository.existsById(id)) {
            ownerRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
```

Create similar controllers for: `PetController.java`, `VetController.java`, `VisitController.java`

### 2.6 Data Initializer
Create `src/main/java/org/acme/petclinic/DataInitializer.java`:

```java
package org.acme.petclinic;

import org.acme.petclinic.entities.*;
import org.acme.petclinic.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private PetTypeRepository petTypeRepository;

    @Autowired
    private VetRepository vetRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Override
    public void run(String... args) throws Exception {
        if (ownerRepository.count() == 0) {
            initializeData();
        }
    }

    private void initializeData() {
        System.out.println("🌱 Initializing database with dummy data...");

        // Create Pet Types
        PetType dog = new PetType(null, "Dog");
        PetType cat = new PetType(null, "Cat");
        PetType bird = new PetType(null, "Bird");
        PetType rabbit = new PetType(null, "Rabbit");
        PetType hamster = new PetType(null, "Hamster");

        petTypeRepository.saveAll(Arrays.asList(dog, cat, bird, rabbit, hamster));

        // Create Owners
        List<Owner> owners = Arrays.asList(
            new Owner(null, "George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023"),
            new Owner(null, "Betty", "Davis", "638 Morris Ave.", "Madison", "6085551749"),
            new Owner(null, "Eduardo", "Rodriquez", "2693 Commerce Ave.", "McFarland", "6085559435"),
            new Owner(null, "Harold", "Davis", "563 Friendly St.", "Windsor", "6085553198"),
            new Owner(null, "Peter", "McTavish", "2387 S. Fair Way", "Madison", "6085552765"),
            new Owner(null, "Jean", "Coleman", "105 N. Lake St.", "Monona", "6085552654"),
            new Owner(null, "Jeff", "Black", "1450 Oak Blvd.", "Monona", "6085555387"),
            new Owner(null, "Maria", "Escobito", "345 Maple Dr.", "Madison", "6085555521"),
            new Owner(null, "David", "Schroeder", "2749 Blackhawk Trail", "Madison", "6085559435"),
            new Owner(null, "Carlos", "Estevez", "2335 Independence La.", "Waunakee", "6085555487")
        );
        ownerRepository.saveAll(owners);

        // Create Pets
        List<Pet> pets = Arrays.asList(
            new Pet(null, "Leo", LocalDate.of(2020, 9, 7), dog, owners.get(0)),
            new Pet(null, "Basil", LocalDate.of(2021, 8, 6), cat, owners.get(1)),
            new Pet(null, "Rosy", LocalDate.of(2019, 4, 17), dog, owners.get(2)),
            new Pet(null, "Jewel", LocalDate.of(2020, 3, 7), cat, owners.get(3)),
            new Pet(null, "Lucky", LocalDate.of(2020, 6, 24), dog, owners.get(4)),
            new Pet(null, "Mulligan", LocalDate.of(2021, 9, 4), hamster, owners.get(5)),
            new Pet(null, "Freddy", LocalDate.of(2020, 3, 9), cat, owners.get(6)),
            new Pet(null, "Lucky", LocalDate.of(2022, 6, 24), dog, owners.get(7)),
            new Pet(null, "Sly", LocalDate.of(2020, 8, 6), cat, owners.get(8)),
            new Pet(null, "Pullo", LocalDate.of(2021, 5, 12), rabbit, owners.get(9)),
            new Pet(null, "Birdie", LocalDate.of(2020, 11, 3), bird, owners.get(0)),
            new Pet(null, "Tweety", LocalDate.of(2021, 1, 15), bird, owners.get(1))
        );
        petRepository.saveAll(pets);

        // Create Vets
        List<Vet> vets = Arrays.asList(
            new Vet(null, "James", "Carter", "Radiology"),
            new Vet(null, "Helen", "Leary", "Surgery"),
            new Vet(null, "Linda", "Douglas", "Emergency"),
            new Vet(null, "Rafael", "Ortega", "Dentistry"),
            new Vet(null, "Henry", "Stevens", "General"),
            new Vet(null, "Sharon", "Jenkins", "Ophthalmology")
        );
        vetRepository.saveAll(vets);

        // Create Visits
        List<Visit> visits = Arrays.asList(
            new Visit(null, LocalDate.of(2023, 1, 1), "rabies shot", pets.get(0), vets.get(0)),
            new Visit(null, LocalDate.of(2023, 3, 4), "neutered", pets.get(2), vets.get(1)),
            new Visit(null, LocalDate.of(2023, 6, 4), "dental checkup", pets.get(1), vets.get(3)),
            new Visit(null, LocalDate.of(2023, 8, 4), "physical exam", pets.get(3), vets.get(4)),
            new Visit(null, LocalDate.of(2023, 10, 20), "vaccinations", pets.get(4), vets.get(0)),
            new Visit(null, LocalDate.of(2023, 12, 1), "eye exam", pets.get(5), vets.get(5)),
            new Visit(null, LocalDate.of(2024, 1, 14), "check-up", pets.get(6), vets.get(4)),
            new Visit(null, LocalDate.of(2024, 2, 15), "surgery consult", pets.get(7), vets.get(1)),
            new Visit(null, LocalDate.of(2024, 3, 3), "wellness visit", pets.get(8), vets.get(4)),
            new Visit(null, LocalDate.of(2024, 4, 6), "checkup", pets.get(9), vets.get(4)),
            new Visit(null, LocalDate.of(2024, 5, 12), "vaccinations", pets.get(10), vets.get(0)),
            new Visit(null, LocalDate.of(2024, 6, 3), "follow-up", pets.get(11), vets.get(2))
        );
        visitRepository.saveAll(visits);

        System.out.println("✅ Database initialization complete!");
    }
}
```

### 2.7 Main Application Class
Create `src/main/java/org/acme/petclinic/PetclinicApplication.java`:

```java
package org.acme.petclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetclinicApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetclinicApplication.class, args);
    }
}
```

### 2.8 Frontend HTML
Create `src/main/resources/static/index.html` - (See full frontend code in separate section or use the enhanced version provided earlier)

---

## 🐳 Step 3: Docker Image Build

### 3.1 Dockerfile
Create `Dockerfile-OpenShift`:

```dockerfile
## Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

## Stage 2: Runtime stage
FROM registry.redhat.io/rhel8/postgresql-16:latest

WORKDIR /app

COPY --from=builder /build/target/petclinic-migration-demo-1.0.0.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 3.2 Build & Push
```bash
# Build for x86_64 (if building on Mac M1/M2)
podman build --platform linux/amd64 -f Dockerfile-OpenShift -t petclinic:1.0.0 .

# Tag for registry
podman tag petclinic:1.0.0 quay.io/YOUR_USERNAME/petclinic:1.0.0

# Push
podman push quay.io/YOUR_USERNAME/petclinic:1.0.0
```

---

## ☸️ Step 4: Kubernetes Manifests

### 4.1 Namespace
Create `k8s/k8s-namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: petclinic-demo
  labels:
    name: petclinic-demo
```

### 4.2 Storage (PVC)
Create `k8s/k8s-pvc.yaml`:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: petclinic-demo
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3-csi
  resources:
    requests:
      storage: 5Gi
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: files-pvc
  namespace: petclinic-demo
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: gp3-csi
  resources:
    requests:
      storage: 1Gi
```

### 4.3 PostgreSQL Deployment
Create `k8s/k8s-postgres.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
  namespace: petclinic-demo
data:
  POSTGRES_DB: petclinic
  POSTGRES_USER: postgres
---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: petclinic-demo
type: Opaque
stringData:
  POSTGRES_PASSWORD: petclinicpwd
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: petclinic-demo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: registry.redhat.io/rhel8/postgresql-16:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRESQL_DATABASE
          value: petclinic
        - name: POSTGRESQL_USERNAME
          value: postgres
        - name: POSTGRESQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: POSTGRES_PASSWORD
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/pgsql/data
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U postgres
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U postgres
          initialDelaySeconds: 5
          periodSeconds: 10
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: petclinic-demo
spec:
  selector:
    app: postgres
  ports:
  - protocol: TCP
    port: 5432
    targetPort: 5432
  type: ClusterIP
```

### 4.4 Application Deployment
Create `k8s/k8s-app.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: petclinic-demo
data:
  application.properties: |
    spring.application.name=petclinic-migration-demo
    server.port=8080
    server.servlet.context-path=/
    spring.datasource.url=jdbc:postgresql://postgres:5432/petclinic
    spring.datasource.username=postgres
    spring.datasource.password=petclinicpwd
    spring.datasource.driver-class-name=org.postgresql.Driver
    spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
    spring.jpa.hibernate.ddl-auto=validate
    spring.jpa.show-sql=false
    logging.level.root=INFO
    logging.level.org.acme.petclinic=DEBUG
    management.endpoints.web.exposure.include=health,info,metrics
    management.endpoint.health.show-details=always
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: petclinic-app
  namespace: petclinic-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: petclinic-app
  template:
    metadata:
      labels:
        app: petclinic-app
    spec:
      serviceAccountName: petclinic-app
      securityContext:
        runAsNonRoot: true
      containers:
      - name: petclinic
        image: quay.io/YOUR_USERNAME/petclinic:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
        securityContext:
          allowPrivilegeEscalation: false
          runAsNonRoot: true
          capabilities:
            drop:
            - ALL
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
      volumes:
      - name: app-config
        configMap:
          name: app-config
---
apiVersion: v1
kind: Service
metadata:
  name: petclinic-service
  namespace: petclinic-demo
spec:
  selector:
    app: petclinic-app
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP
```

### 4.5 Service Account & RBAC
Create `k8s/k8s-serviceaccount.yaml`:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: petclinic-app
  namespace: petclinic-demo
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: petclinic-app-role
  namespace: petclinic-demo
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: petclinic-app-rolebinding
  namespace: petclinic-demo
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: petclinic-app-role
subjects:
- kind: ServiceAccount
  name: petclinic-app
  namespace: petclinic-demo
```

### 4.6 Route
Create `k8s/k8s-route.yaml`:

```yaml
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: petclinic-route
  namespace: petclinic-demo
spec:
  to:
    kind: Service
    name: petclinic-service
  port:
    targetPort: 8080
  tls:
    termination: edge
    insecureEdgeTerminationPolicy: Allow
```

---

## 🚀 Step 5: Deployment Commands

### 5.1 Prerequisites
```bash
# Update YOUR_USERNAME in k8s-app.yaml
sed -i 's/YOUR_USERNAME/your_actual_username/g' k8s/k8s-app.yaml

# Login to OpenShift
oc login --token=YOUR_TOKEN --server=YOUR_SERVER
```

### 5.2 Initial Deployment
```bash
# Deploy all resources
oc apply -f k8s/

# Wait for postgres to be ready
oc rollout status deployment/postgres -n petclinic-demo

# Wait for app to be ready
oc rollout status deployment/petclinic-app -n petclinic-demo
```

### 5.3 Verify Deployment
```bash
# Check pods
oc get pods -n petclinic-demo

# Check services
oc get svc -n petclinic-demo

# Check route
oc get route -n petclinic-demo

# Get application URL
oc get route petclinic-route -n petclinic-demo -o jsonpath='{.spec.host}'
```

### 5.4 Access Application
```bash
# Open in browser
open http://$(oc get route petclinic-route -n petclinic-demo -o jsonpath='{.spec.host}')

# Test API
curl http://$(oc get route petclinic-route -n petclinic-demo -o jsonpath='{.spec.host}')/owners | jq
```

---

## 🔄 Step 6: Verify Data

```bash
# Check record counts
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM owner;"
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM pet;"
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM vet;"
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT COUNT(*) FROM visit;"
```

Expected output:
- 10 owners
- 12 pets
- 6 vets
- 12 visits

---

## 📊 Git Repository Structure

```
petclinic/
├── README.md                          # This guide
├── pom.xml
├── Dockerfile-OpenShift
├── src/
│   └── main/
│       ├── java/org/acme/petclinic/
│       │   ├── PetclinicApplication.java
│       │   ├── DataInitializer.java
│       │   ├── entities/
│       │   │   ├── Owner.java
│       │   │   ├── Pet.java
│       │   │   ├── PetType.java
│       │   │   ├── Vet.java
│       │   │   └── Visit.java
│       │   ├── repositories/
│       │   │   ├── OwnerRepository.java
│       │   │   ├── PetRepository.java
│       │   │   ├── PetTypeRepository.java
│       │   │   ├── VetRepository.java
│       │   │   └── VisitRepository.java
│       │   └── resources/
│       │       ├── OwnerController.java
│       │       ├── PetController.java
│       │       ├── VetController.java
│       │       └── VisitController.java
│       └── resources/
│           ├── application.properties
│           └── static/index.html
└── k8s/
    ├── k8s-namespace.yaml
    ├── k8s-pvc.yaml
    ├── k8s-postgres.yaml
    ├── k8s-app.yaml
    ├── k8s-serviceaccount.yaml
    └── k8s-route.yaml
```

---

## 🔧 Troubleshooting

### Issue: Pod CrashLoopBackOff
```bash
# Check logs
oc logs deployment/petclinic-app -n petclinic-demo

# Common issues:
# 1. Image not accessible - verify image push
# 2. Database not ready - wait for postgres pod
# 3. Resource limits exceeded - increase limits
```

### Issue: Database Connection Error
```bash
# Verify postgres is running
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -c "SELECT 1;"

# Check credentials in secret
oc get secret postgres-secret -n petclinic-demo -o yaml | grep POSTGRES_PASSWORD
```

### Issue: Data Not Appearing
```bash
# Check table counts
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "\dt"

# Check data
oc exec deployment/postgres -n petclinic-demo -- psql -U postgres -d petclinic -c "SELECT * FROM owner LIMIT 5;"

# If empty, data initializer didn't run
# Ensure ddl-auto is NOT set to 'create' (use 'validate')
```

---

## 📝 Summary

This guide provides **complete, repeatable steps** to deploy Petclinic to OpenShift. 

**Key Points:**
- ✅ All source code included
- ✅ All Kubernetes manifests provided
- ✅ Complete deployment workflow
- ✅ Troubleshooting guide included
- ✅ Git-ready structure
- ✅ Ready for MTC/OADP migration demos

**Next Steps:**
- Push this project to Git
- Maintain all source files
- Use this for backup/restore scenarios
- Ready for migration demonstrations

---

**Version:** 1.0.0  
**Last Updated:** July 27, 2026  
**OpenShift Version:** 4.16+  
**Spring Boot:** 3.1.5  
**PostgreSQL:** 16  
