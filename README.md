# workload-api-java

A gRPC server and client built with Java and Maven, implementing the `Greeter` service secured with mutual TLS using [SPIFFE](https://spiffe.io/) credentials.

## Prerequisites

- [SDKMAN!](https://sdkman.io/) (recommended for managing Java and Maven)
- Java 25+
- Maven 3.9+

Install Java and Maven via SDKMAN!:

```bash
sdk install java 25.0.1-tem
sdk install maven 3.9.12
```

## SPIFFE Workload API

Both the server and client obtain their TLS credentials (X.509 SVID and trust bundle) from the SPIFFE Workload API, configured via the `SPIFFE_ENDPOINT_SOCKET` environment variable.

The server and client enforce mutual TLS and require the peer's SPIFFE ID to match the value set in the `ACCEPTED_SPIFFE_ID` environment variable. Both will fail to start if `ACCEPTED_SPIFFE_ID` is not set.

## Getting Started

Clone the repository and navigate into the project directory:

```bash
git clone <repository-url>
cd workload-api-java
```

### Build

Compiles the application and generates Java sources from the proto file:

```bash
mvn compile
```

### Test

```bash
mvn test
```

### Package

Builds a self-contained fat JAR with all dependencies bundled:

```bash
mvn package
```

The JAR will be output to `target/workload-api-java-1.0-SNAPSHOT.jar`.

## Docker

Multi-arch images (AMD64 and ARM64) are built using [Docker Buildx](https://docs.docker.com/buildx/working-with-buildx/). Each image bundles the fat JAR and a Java 25 JRE.

### Setup Buildx

```bash
docker buildx create --use --name multiarch
docker buildx inspect --bootstrap
```

### Build and push the server image

```bash
REGISTRY=<registry>
VERSION=1.0.0

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f Dockerfile.server \
  -t ${REGISTRY}/hello-world-server:${VERSION} \
  --push \
  .
```

### Build and push the client image

```bash
REGISTRY=<registry>
VERSION=1.0.0

docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -f Dockerfile.client \
  -t ${REGISTRY}/hello-world-client:${VERSION} \
  --push \
  .
```

## gRPC API

The server exposes the following service defined in `src/main/proto/greeter.proto`:

```protobuf
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

Java sources for the service are generated automatically from the proto file during `mvn compile` and output to `target/generated-sources/`.

### Example: calling SayHello with grpcurl

With the server running, you can invoke the `SayHello` RPC using [grpcurl](https://github.com/fullstorydev/grpcurl). Since the server requires mTLS, you must supply a client certificate and key from the SPIFFE Workload API:

```bash
grpcurl \
  -insecure \
  -cert /var/run/secrets/workload-spiffe-credentials/certificates.pem \
  -key /var/run/secrets/workload-spiffe-credentials/private_key.pem \
  -d '{"name": "World"}' \
  localhost:50051 com.example.Greeter/SayHello
```

> **Note:** `-insecure` is required because the server's certificate uses a SPIFFE URI SAN (`spiffe://...`) rather than a DNS SAN, which causes standard hostname verification to fail. The client certificate and key are still presented for mutual TLS.

Expected response:

```json
{
  "message": "Hello World"
}
```

## Kubernetes

Both the server and client can be deployed on GKE using the [workload-api-shim](https://github.com/larkintuckerllc/workload-api-shim) as a sidecar init container to supply SPIFFE credentials via the Fleet Workload Identity CSI driver.

### Server Pod and Service

```yaml
apiVersion: v1
kind: Pod
metadata:
  namespace: debug
  name: grpc-server
  labels:
    app: grpc-server
spec:
  serviceAccountName: default
  initContainers:
  - name: workload-api-shim
    image: gcr.io/jtucker-wia-f/workload-api-shim:0.1.1
    args:
    - --socket-path=/run/spiffe/workload.sock
    - --creds-dir=/var/run/secrets/workload-spiffe-credentials
    restartPolicy: Always
    volumeMounts:
    - name: spiffe-socket
      mountPath: /run/spiffe
    - name: fleet-spiffe-credentials
      mountPath: /var/run/secrets/workload-spiffe-credentials
      readOnly: true
  containers:
  - name: grpc-server
    image: gcr.io/jtucker-wia-f/hello-world-server:0.1.0
    env:
    - name: ACCEPTED_SPIFFE_ID
      value: spiffe://jtucker-wia-f.svc.id.goog/ns/debug/sa/default
    - name: SPIFFE_ENDPOINT_SOCKET
      value: unix:/run/spiffe/workload.sock
    ports:
    - containerPort: 50051
    volumeMounts:
    - name: spiffe-socket
      mountPath: /run/spiffe
  volumes:
  - name: spiffe-socket
    emptyDir: {}
  - name: fleet-spiffe-credentials
    csi:
      driver: podcertificate.gke.io
      volumeAttributes:
        signerName: spiffe.gke.io/fleet-svid
        trustDomain: fleet-project/svc.id.goog
---
apiVersion: v1
kind: Service
metadata:
  namespace: debug
  name: grpc-server
spec:
  selector:
    app: grpc-server
  ports:
  - port: 50051
    targetPort: 50051
```

Key points:
- The `workload-api-shim` init container runs as a sidecar (`restartPolicy: Always`) and serves the SPIFFE Workload API on a shared Unix socket
- SPIFFE credentials are mounted from the GKE Fleet Workload Identity CSI driver (`podcertificate.gke.io`)
- The `grpc-server` container reads the socket via `SPIFFE_ENDPOINT_SOCKET`
- `ACCEPTED_SPIFFE_ID` restricts connections to clients presenting the specified SPIFFE ID
- The Service exposes port `50051` for in-cluster access

### Client Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  namespace: debug
  name: grpc-client
spec:
  serviceAccountName: default
  restartPolicy: Never
  initContainers:
  - name: workload-api-shim
    image: gcr.io/jtucker-wia-f/workload-api-shim:0.2.0
    args:
    - --socket-path=/run/spiffe/workload.sock
    - --creds-dir=/var/run/secrets/workload-spiffe-credentials
    restartPolicy: Always
    volumeMounts:
    - name: spiffe-socket
      mountPath: /run/spiffe
    - name: fleet-spiffe-credentials
      mountPath: /var/run/secrets/workload-spiffe-credentials
      readOnly: true
  containers:
  - name: grpc-client
    image: gcr.io/jtucker-wia-f/hello-world-client:0.3.0
    env:
    - name: ACCEPTED_SPIFFE_ID
      value: spiffe://jtucker-wia-f.svc.id.goog/ns/debug/sa/default
    - name: GRPC_SERVER_HOST
      value: grpc-server
    - name: SPIFFE_ENDPOINT_SOCKET
      value: unix:/run/spiffe/workload.sock
    volumeMounts:
    - name: spiffe-socket
      mountPath: /run/spiffe
  volumes:
  - name: spiffe-socket
    emptyDir: {}
  - name: fleet-spiffe-credentials
    csi:
      driver: podcertificate.gke.io
      volumeAttributes:
        signerName: spiffe.gke.io/fleet-svid
        trustDomain: fleet-project/svc.id.goog
```

Key points:
- `restartPolicy: Never` is set at the Pod spec level so the client does not restart after it exits
- `restartPolicy: Always` on the `workload-api-shim` init container is the Kubernetes sidecar pattern and is independent of the Pod-level policy
- `GRPC_SERVER_HOST` is set to `grpc-server`, resolving to the server Service within the cluster
- `ACCEPTED_SPIFFE_ID` restricts connections to servers presenting the specified SPIFFE ID

## Project Structure

```
workload-api-java/
├── pom.xml
├── Dockerfile.server
├── Dockerfile.client
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   ├── HelloWorldClient.java
    │   │   └── HelloWorldServer.java
    │   └── proto/
    │       └── greeter.proto
    └── test/java/com/example/
        └── HelloWorldServerTest.java
```
