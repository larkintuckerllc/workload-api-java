# workload-api-java

A gRPC server built with Java and Maven, implementing the `Greeter` service.

## Prerequisites

- [SDKMAN!](https://sdkman.io/) (recommended for managing Java and Maven)
- Java 25+
- Maven 3.9+

Install Java and Maven via SDKMAN!:

```bash
sdk install java 25.0.1-tem
sdk install maven 3.9.12
```

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

### Run

```bash
mvn exec:java
```

The server will start on port `50051`.

### Test

```bash
mvn test
```

### Package

Build a JAR file:

```bash
mvn package
```

The JAR will be output to `target/workload-api-java-1.0-SNAPSHOT.jar`.

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

With the server running, you can invoke the `SayHello` RPC using [grpcurl](https://github.com/fullstorydev/grpcurl):

```bash
grpcurl -plaintext -d '{"name": "World"}' localhost:50051 com.example.Greeter/SayHello
```

Expected response:

```json
{
  "message": "Hello World"
}
```

## Project Structure

```
workload-api-java/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/
    │   │   └── App.java
    │   └── proto/
    │       └── greeter.proto
    └── test/java/com/example/
        └── AppTest.java
```
