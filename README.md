## Hands-on System Design: Distributed Log Processing with Java & Spring Boot

curriculum : https://sdcourse.substack.com/p/curriculum-distributed-log-implementation

# Why This Course?
Every app you’ve ever used—Netflix buffering your show, Uber tracking your ride, Instagram loading your feed—generates logs. Millions of them. Every second.

But here’s what they don’t teach you in school: collecting logs is the easy part. The hard part? **Processing 100 million log events per second without losing a single one**, querying them in real-time, and doing it all while your system stays up 99.99% of the time.

This course bridges the gap between “I can code” and “I can build systems that power billion-dollar companies.” You’ll build a production-grade distributed log processing platform from scratch—the same architecture pattern used by Cloudflare, Datadog, and Elasticsearch.

## What You’ll Build
By the end of this course, you’ll have built **LogStream** - a fully functional distributed log processing platform capable of:

* **Ingesting** 10,000+ log events per second from multiple sources.
* **Processing** logs in real-time with custom parsing and enrichment.
* **Storing** petabytes of log data efficiently.
* **Querying** logs with sub-second latency.
* **Alerting** on patterns and anomalies automatically.
* **Scaling** horizontally to handle traffic spikes.

You’ll deploy this to AWS/GCP with monitoring, alerting, and auto-scaling. This isn’t a toy project—it’s a portfolio piece that demonstrates senior-level system design skills.

---

## Who Should Take This Course?

### ✅ You’ll thrive here if you:
* Can write basic Java code and understand Spring Boot basics.
* Want to transition from feature development to infrastructure/platform roles.
* Need to architect systems that handle massive scale.
* Are preparing for senior engineer or architect interviews.
* Work in observability, SRE, or data engineering teams.

### ❌ You’ll struggle if you:
* Haven’t written Java before (start with basics first).
* Expect theory without implementation (this is 80% coding).
* Want quick wins without debugging production issues.

---

## What Makes This Course Different?

1. **You Write Every Line of Code**
   No copy-pasting from GitHub. No “download the starter code.” You’ll type every character, hit every error, and debug every issue. That’s how muscle memory builds.

2. **Production Failures Are Part of the Curriculum**
   We’ll intentionally break things—simulate network partitions, disk failures, memory leaks—and you’ll fix them. Because production systems fail, and you need to know why.

3. **Real Numbers, Real Trade-offs**
   When we choose Kafka over RabbitMQ, you’ll see the actual throughput numbers, latency percentiles, and cost implications. No hand-waving.

4. **From Localhost to Cloud**
   You’ll start on your laptop and end with a multi-region deployment on AWS. You’ll see exactly where complexity creeps in and why “it works on my machine” is meaningless.

---

## Key Topics Covered

### Foundation Layer
* Event-driven architecture patterns
* Log anatomy and structured logging
* Network protocols for log ingestion (TCP, HTTP, gRPC)
* Serialization formats (JSON, Protocol Buffers, Avro)

### Distribution Layer
* Apache Kafka internals and configuration
* Consumer groups and partition rebalancing
* Exactly-once semantics vs at-least-once
* Back-pressure handling and flow control

### Processing Layer
* Stream processing vs batch processing
* Stateful transformations and windowing
* Schema registry and evolution
* Custom parsing engines

### Storage Layer
* Time-series database design
* Columnar storage formats (Parquet)
* Index strategies (inverted indexes, bloom filters)
* Data retention and lifecycle policies

### Query Layer
* Distributed query execution
* Query optimization techniques
* Caching strategies
* Rate limiting and query quotas

### Operational Excellence
* Observability for observability systems (meta-monitoring)
* Capacity planning and cost optimization
* Multi-tenancy and resource isolation
* Disaster recovery and data replay

---

## Prerequisites

### Must Have:
* **Java 11+ proficiency** (streams, lambdas, concurrency)
* **Spring Boot basics** (REST APIs, dependency injection)
* **SQL fundamentals**
* **Git and command-line comfort**
* **Docker basics** (we’ll deepen this)

### Nice to Have:
* Basic AWS/GCP experience
* Understanding of HTTP protocols
* Exposure to message queues
* Linux system administration

### Required Setup:
* Machine with 16GB RAM (8GB minimum, but you’ll suffer)
* IntelliJ IDEA or VS Code
* Docker Desktop
* AWS/GCP free tier account (for final deployment)

## Course Structure
The course is organized into **6 major sections** spanning **16 weeks**, with approximately **48 hands-on coding lessons**. Each section builds a complete layer of the system.
