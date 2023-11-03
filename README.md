# Build Template Examples

This repository contains opinionated build templates for JeKa.

By 'build template,' we refer to KBeans already configured for building a certain kind of project by setting specific parameters.

For instance, an organization might create a build template for building, testing, and deploying Spring-Boot applications on Kubernetes.

Each Spring-Boot project uses this template, requiring only the specification of its name and dependencies. 
The build templates then define compilation tests with coverage, analysis for Sonarqube, and deployment on different environments.

Build templates are usually managed by a central 'platform team' that is responsible for upgrading dependency versions 
and adjusting templates to the current infrastructure.
