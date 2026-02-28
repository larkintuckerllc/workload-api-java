# workload-api-java

A Java application built with Maven.

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

```bash
mvn compile
```

### Run

```bash
java -cp target/classes com.example.App
```

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

## Project Structure

```
workload-api-java/
├── pom.xml
└── src/
    ├── main/java/com/example/
    │   └── App.java
    └── test/java/com/example/
        └── AppTest.java
```
