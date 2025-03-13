# Build Template Examples

This repository provides reusable build templates for JeKa.

A build template refers to pre-configured KBeans for setting up complete CI/CD pipelines. 

For example, a template may handle tasks like building a Spring Boot application and its Angular front-end, 
running static analysis, deploying to a test environment, performing functional tests, 
and promoting the build to a staged environment.

Let's look at a concrete example:

## Springboot + ReactJs + Sonarqube analysis + end-to-end tests

[This template](jeka-src/dev/jeka/demo/templates/springboot/reactjs/Template.java) defines a build process for
Spring Boot projects that may optionally include a ReactJS frontend.

The build performs the following tasks:
- Compiles the code and runs Java tests with coverage.
- Builds and unit-tests the ReactJS frontend if present.
- Executes SonarQube analysis for both Java and JavaScript code.
- Produces a Spring-Boot JAR that includes both the backend and the frontend.
- Docker and native images can be created optionally.

To use this build, projects only need to add the following snippet in their *local.properties* file.

```properties
jeka.classpath=dev.jeka:template-examples:0.11.20-1
@template=
```

Run a full CI build with `jeka project: pack template: e2e sonar`.  

[Check this project](https://github.com/jeka-dev/demo-build-templates-consumer.git) for an example.



