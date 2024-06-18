# Build Template Examples

This repository contains opinionated build templates for JeKa.

By build template, we refer to reusable KBeans designed to perform one or more tasks.

For instance, consider a pipeline that builds a Spring Boot application and its Angular front-end, runs static analysis, deploys it to a test environment, runs functional tests, and then promotes it to a staged environment.

This involves specifying many steps, so we aim to centralize this logic. Projects needing to follow this pipeline can simply reference the template and override only the specifics.

With JeKa, there are many ways to achieve this goal. While it's possible to pack this logic into a vanilla Java library that projects can use in their build scripts, we focus on creating reusable KBeans that projects can leverage without writing a single line of code.

Let's look at a concrete example:

## Springboot + ReactJs

[This template](src/dev/jeka/demo/templates/SpringBootTemplateBuild.java) defines a build for 
Spring-Boot projects that optionally contain a ReactJs frontend.

The build actually compiles, runs tests with coverage, builds reactJs, performs Sonarqube analysis and produces a bootable jar,
containing the both backend and frontend.

To leverage this build, projects has only to supply the following *local.properties* file. Spring-Boot application dependencies 
are specified in *project-dependencies.txt*.

```properties
# Import the template in the classpath
jeka.classpath.inject=dev.jeka:template-examples:0.10.45.0
jeka.default.kbean=dev.jeka.demo.templates.SpringBootTemplateBuild
# Set project specific values
jeka.java.version=21
kb#springbootVersion=3.1.5
```

The full build is triggered with command: `jeka #packQuality` provided by the template, while the built jar 
can be run using `jeka #runJar`.

[See this project](https://github.com/jeka-dev/working-examples/tree/master/templated) to get a concrete usage example.



