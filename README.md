# Build-Convention Examples

This repository provides **build-conventions** for use with **JeKa**.

A **build-convention** is a preconfigured `KBean` that encapsulates best practices and standard tasks for building, 
testing, and deploying applications according to a given structure or technology stack.

These conventions simplify project setup by providing ready-to-use build logic for common scenarios.

For example, a build convention can orchestrate tasks such as:
- Building a Spring Boot application and its optional frontend (e.g. Angular or React),
- Running static analysis (e.g. SonarQube),
- Deploying to a test environment,
- Performing functional/end-to-end tests,
- Promoting artifacts to a staging or production environment.

---

## 📦 Example: Spring Boot + ReactJS + SonarQube + End-to-End Tests

The template defined in  
[`Convention.java`](jeka-src/dev/jeka/demo/conventions/springboot/reactjs/Convention.java)  
provides a build process for **Spring Boot** projects that optionally include a **ReactJS frontend**.

This convention automates the following:

- Compiles the Java backend and runs unit tests with code coverage.
- Builds and tests the ReactJS frontend (if present).
- Runs **SonarQube** analysis for both Java and JavaScript code.
- Produces a Spring Boot executable JAR that includes both backend and frontend assets.
- Optionally builds **Docker** and **native images**.

### 🛠 Usage

To adopt this convention in your project, simply add the following line to your `jeka.properties` file:
.

```properties
jeka.classpath=dev.jeka:convention-examples:0.11.39-2
@template=on
```

Run a full CI build with: `jeka build`.  

[Check this project](https://github.com/jeka-dev/demo-build-conventions-consumer.git) for an example.



