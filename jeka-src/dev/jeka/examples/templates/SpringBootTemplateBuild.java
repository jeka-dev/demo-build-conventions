package dev.jeka.examples.templates;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.nodejs.JkNodeJs;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.springboot.JkSpringbootProject;


import java.nio.file.Files;


@JkDoc("""
        Builds a Spring-Boot project, optionally containing a reactjs frontend.
        This build handles Java compilation, Junit testing with coverage, reactjs build, Sonarqube analysis.
        
        This template is designed to be rigid for enforcing a common usage of tools and layout.
        TThe project dependencies are supposed to be declared in <i>jeka/project-dependencies.txt</i> file.
        
        The project version, along the SonarQube host/token props, are expected to be injected by the CI tool.
        """)
public class SpringBootTemplateBuild extends KBean {

    @JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent:")
    public static final String JACOCO_VERSION = "0.8.11";

    @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:")
    public static final String SONARQUBE_VERSION = "5.0.1.3006";

    public static final String NODEJS_VERSION = "20.10.0";

    public static final String REACTJS_BASE_DIR = "reactjs-client";

    @JkDepSuggest(versionOnly = true, hint = "org.springframework.boot:spring-boot-starter-parent:")
    @JkDoc("Spring-Boot version")
    public String springbootVersion = "3.2.0";

    // Do not enforce to use a specific of NodeJs. Propose latest LTS versions instead.
    @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2")
    public String nodeJsVersion = NODEJS_VERSION;

    @JkDoc("The project key formatted as group:name that will be used for naming artifacts.")
    public String moduleId = "org.myorg:" + getBaseDir().toAbsolutePath().getFileName();

    @JkDoc("Project version injected by CI/CD tool")
    @JkInjectProperty("PROJECT_VERSION")
    public String projectVersion;

    private final JkProject project = load(ProjectKBean.class).project;

    @Override
    protected void init() {

        project.setModuleId(moduleId);
        project.setVersion(projectVersion);

        // Configure project as a Spring-Boot application
        JkSpringbootProject.of(project).includeParentBom(springbootVersion).configure();

        // Configure project for running test with Jacoco coverage
        JkJacoco.ofVersion(JACOCO_VERSION).configureForAndApplyTo(project);

        // Build reactJs if 'reactjs-client' dir is present.
        // The bundled js app is copied in 'resources/static'.
        if (hasReactJsDir()) {
            JkNodeJs.ofVersion(nodeJsVersion).configure(project, REACTJS_BASE_DIR, "build",
                    "npx yarn install ", "npm run build");
        }
    }

    @JkDoc("Run a Sonarqube analysis.")
    public void runSonarqube() {
        sonarqubeBase()
                .configureFor(project)

                // applies properties declared in local.properties and starting with '.sonar' prefix'
                .setProperties(getRunbase().getProperties().getAllStartingWith("sonar.", true))
                .run();

        // Apply sonarQQube analysis on NodeJs code as well, if present.
        if (hasReactJsDir()) {
            sonarqubeBase()
                    .setProperty(JkSonarqube.PROJECT_KEY, project.getModuleId().toString() + "-js")
                    .setProperty(JkSonarqube.PROJECT_VERSION, projectVersion)
                    .setProperty(JkSonarqube.LANGUAGE, "javascript")
                    .setProperty(JkSonarqube.SOURCES, REACTJS_BASE_DIR)
                    .setProperty("exclusions", "node_modules")
                    .setProperties(getRunbase().getProperties())
                    .run();
        }
    }

    private boolean hasReactJsDir() {
        return Files.exists(getBaseDir().resolve(REACTJS_BASE_DIR));
    }

    private JkSonarqube sonarqubeBase() {
        return JkSonarqube.ofVersion(getRunbase().getDependencyResolver(), SONARQUBE_VERSION);
    }

}
