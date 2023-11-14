# Build Template Examples

This repository contains opinionated build templates for JeKa.

By 'build template,' we refer to KBeans already configured for building a certain kind of project by setting specific parameters.

For instance, an organization might create a build template for building, testing, and deploying Spring-Boot applications on Kubernetes.

Each Spring-Boot project uses this template, requiring only the specification of its name and dependencies. 
The build templates then define compilation tests with coverage, analysis for Sonarqube, and deployment on different environments.

Build templates are usually managed by a central 'platform team' that is responsible for upgrading dependency versions 
and adjusting templates to the current infrastructure.

## Springboot + ReactJs

[This template](./src/main/java/dev/jeka/examples/templates/SpringBootTemplateBuild.java) defines a build for 
Spring-Boot projects that optionally contain a ReactJs frontend.

The build actually compiles, runs tests with coverage, builds reactJs, performs Sonarqube analysis and produces a bootable jar.

To leverage this build, projects has only to supply the following *local.properties* file.
The full build is triggered with command: `jeka #buildQuality` provided by the template.

The built jar can be run using `jeka #runJar`

```properties
# Import the template in the classpath
jeka.classpath.inject=dev.jeka:template-examples:0.10.38.0
jeka.default.kbean=dev.jeka.examples.templates.SpringBootTemplateBuild

# Set project specific values
jeka.java.version=21
kb#springbootVersion=3.1.5
kb#moduleId=org.example:jeka-templated-project
```

