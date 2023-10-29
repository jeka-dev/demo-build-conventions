package org.github.djeang.buildtemplates;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.java.JkJavaProcess;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.nodejs.JkNodeJs;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.springboot.JkSpringbootProjectAdapter;

import java.nio.file.Files;
import java.nio.file.Path;

public class SpringBootTemplateBuild extends JkBean implements JkIdeSupport.JkSupplier {

    public static final String JACOCO_VERSION = "0.8.8";

    public static final String SONARQUBE_VERSION = "5.0.1.3006";

    public static final String NODEJS_VERSION = "20.9.0";

    public static final String REACTJS_BASE_DIR = "reactjs-client";

    @JkDepSuggest(versionOnly = true, hint = "org.springframework:org.springframework.boot:spring-boot:")
    @JkDoc("Spring-Boot version")
    public String springbootVersion = "3.1.4";

    @JkDoc("The project key formatted as group:name that will bbe used for naming artifacts and outpouts")
    public String moduleId;

    @JkDoc("Project version injected by CI/CD tool")
    @JkInjectProperty("PROJECT_VERSION")
    public String projectVersion;

    private SpringBootTemplateBuild() {
        moduleId = "org.myorg:" + getBaseDir().toAbsolutePath().getFileName();
    }

    @JkDoc("Performs a simple build, without code coverage")
    public void build() {
        project().clean().pack();
    }

    @JkDoc("Performs a build including quality static analysis.")
    public void buildQuality() {
        JkProject project = project();
        JkJacoco.ofVersion(getRuntime().getDependencyResolver(), JACOCO_VERSION)
                .configureForAndApplyTo(project);
        project.clean().pack();
        sonarqubeBase()
                .configureFor(project)
                .setProperties(getRuntime().getProperties())  // applies properties declared in local.properties and starting with '.sonar' prefix'
                .run();
        if (Files.exists(reactBaseDir())) {
            sonarqubeBase()
                    .setProperty(JkSonarqube.PROJECT_KEY, project.publication.getModuleId().getColonNotation() + "-js")
                    .setProperty(JkSonarqube.PROJECT_VERSION, projectVersion)
                    .setProperty(JkSonarqube.LANGUAGE, "javascript")
                    .setProperty(JkSonarqube.SOURCES, REACTJS_BASE_DIR)
                    .setProperty("exclusions", "node_modules")
                    .setProperties(getRuntime().getProperties())
                    .run();
        }
    }

    public void runJar() {
        JkProject project = project();
        JkJavaProcess.ofJavaJar(project.artifactProducer.getMainArtifactPath()).setDestroyAtJvmShutdown(true).run();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }

    private JkProject project() {
        JkProject project = JkProject.of();
        project.publication.setModuleId(moduleId);
        project.publication.setVersion(projectVersion);
        project.packaging.manifest.addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, projectVersion);
        JkSpringbootProjectAdapter.of()
                .setCreateOriginalJar(false)
                .setSpringbootVersion(springbootVersion)
                .configure(project);

        // Build reactJs if present
        if (Files.exists(reactBaseDir())) {
            JkNodeJs.ofVersion(NODEJS_VERSION).configure(project, REACTJS_BASE_DIR, "build",
                    "npx yarn install ", "npm run build");
        }
        return project;
    }

    private Path reactBaseDir() {
        return getBaseDir().resolve(REACTJS_BASE_DIR);
    }

    private JkSonarqube sonarqubeBase() {
        return JkSonarqube.ofVersion(getRuntime().getDependencyResolver(), SONARQUBE_VERSION)
                .setProperty(JkSonarqube.HOST_URL, "http://localhost:9000")
                .setProperty("token", "sqa_ae771fbb270773bc8478c87a2ac684e7d9cfc0fa");
    }


}
