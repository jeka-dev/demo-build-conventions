package dev.jeka.demo.templates;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.nodejs.JkNodeJs;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.springboot.JkSpringbootProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JkDoc("""
        Builds a Spring-Boot project, optionally containing a reactjs frontend.
        This build handles Java compilation, Junit testing with coverage, reactjs build, Sonarqube analysis.
        
        This template is voluntary designed to be rigid for enforcing conventions.
        Only applicationId and nodeJs version can be overridden by users.
        
        The project version, along the SonarQube host/token props, are expected to be injected by the CI tool.
        """)
public class SpringBootTemplateBuild extends KBean implements JkIdeSupportSupplier {

    @JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent:")
    public static final String JACOCO_VERSION = "0.8.11";

    @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:")
    public static final String SONARQUBE_VERSION = "5.0.1.3006";

    public static final String NODEJS_VERSION = "20.10.0";

    public static final String REACTJS_BASE_DIR = "reactjs-client";

    // Do not enforce to use a specific of NodeJs. Propose latest LTS versions instead.
    @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2")
    public String nodeJsVersion = NODEJS_VERSION;

    @JkDoc("The unique application id within the organization. By defauly values to root dir name.")
    public String appId = getBaseDir().toAbsolutePath().getFileName().toString();

    @JkDoc("Project version injected by CI/CD tool")
    @JkInjectProperty("PROJECT_VERSION")
    private String projectVersion;

    @JkDoc("Performs a simple build, without code coverage")
    public void pack() {
        project().clean().pack();
    }

    @JkDoc("Performs a build including quality static analysis.")
    public void packQuality() {
        JkProject project = project();
        JkJacoco.ofVersion(getRunbase().getDependencyResolver(), JACOCO_VERSION)
                .configureFor(project);
        project.clean().pack();
        sonarqubeBase()
                .configureFor(project)

                // applies properties declared in jeka.properties and starting with 'sonar.' prefix
                // sonar properties as sonar.host are supposed to be injected
                .setProperties(getRunbase().getProperties().getAllStartingWith("sonar.", true))
                .run();

        // Apply sonarQQube analysis on NodeJs code as well, if present.
        if (Files.exists(reactBaseDir())) {
            sonarqubeBase()
                    .setProperty(JkSonarqube.PROJECT_KEY, project.getModuleId()+ "-js")
                    .setProperty(JkSonarqube.PROJECT_VERSION, projectVersion)
                    .setProperty(JkSonarqube.LANGUAGE, "javascript")
                    .setProperty(JkSonarqube.SOURCES, REACTJS_BASE_DIR)
                    .setProperty("exclusions", "node_modules")
                    .setProperties(getRunbase().getProperties())
                    .run();
        }
    }

    @JkDoc("Executes the built bootable jar")
    public void runJar() {
        project().prepareRunJar(JkProject.RuntimeDeps.EXCLUDE).run();
    }

    @JkDoc("Displays the dependency tree on the console.")
    public void depTree() {
        project().displayDependencyTree();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project().getJavaIdeSupport();
    }

    private JkProject project() {
        JkProject project = JkProject.of();
        project.setModuleId(appId);
        project.setVersion(projectVersion);
        project.packaging.getManifest().addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, projectVersion);

        // Configure project as a Spring-Boot application
        JkSpringbootProject.of(project).configure();

        // Build reactJs if 'reactjs-client' dir is present.
        // The bundled js app is copied in 'resources/static'.
        if (Files.exists(reactBaseDir())) {
            JkNodeJs.ofVersion(nodeJsVersion).configure(
                    project,
                    REACTJS_BASE_DIR,
                    "build",
                    "static",
                    List.of("npx yarn install ", "npm run build"),
                    List.of());
        }
        return project;
    }

    private Path reactBaseDir() {
        return getBaseDir().resolve(REACTJS_BASE_DIR);
    }

    private JkSonarqube sonarqubeBase() {
        return JkSonarqube.ofVersion(getRunbase().getDependencyResolver(), SONARQUBE_VERSION);
    }

}
