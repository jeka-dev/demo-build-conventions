package dev.jeka.demo.templates;

import dev.jeka.core.api.depmanagement.JkDepSuggest;
import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.project.JkIdeSupport;
import dev.jeka.core.api.project.JkIdeSupportSupplier;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkApplicationTester;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.KBean;
import dev.jeka.plugins.jacoco.JkJacoco;
import dev.jeka.plugins.nodejs.JkNodeJs;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.springboot.JkSpringbootProject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@JkDoc("""
        Builds a Spring-Boot project, optionally containing a reactjs frontend.
        This build handles Java compilation, Junit testing with coverage, reactjs build, Sonarqube analysis.
        
        This template is voluntary designed to be rigid for enforcing conventions.
        Only applicationId and nodeJs version can be overridden by users.
        
        The project version, along the SonarQube host/token props, are expected to be injected by the CI tool.
        """)
@JkInjectClasspath("dev.jeka:nodejs-plugin")
@JkInjectClasspath("dev.jeka:sonarqube-plugin")
@JkInjectClasspath("dev.jeka:jacoco-plugin")
@JkInjectClasspath("dev.jeka:springboot-plugin")
public class SpringBootTemplateBuild extends KBean implements JkIdeSupportSupplier {

    @JkDepSuggest(versionOnly = true, hint = "org.jacoco:org.jacoco.agent:")
    public static final String JACOCO_VERSION = "0.8.11";

    @JkDepSuggest(versionOnly = true, hint = "org.sonarsource.scanner.cli:sonar-scanner-cli:")
    public static final String SONARQUBE_VERSION = "5.0.1.3006";

    public static final String NODEJS_VERSION = "20.10.0";

    public static final String REACTJS_BASE_DIR = "reactjs-client";

    static final String E2E_TEST_PATTERN = "^e2e\\..*";

    // Do not enforce to use a specific of NodeJs. Propose latest LTS versions instead.
    @JkDepSuggest(versionOnly = true, hint = "20.10.0,18.19.0,16.20.2")
    public String nodeJsVersion = NODEJS_VERSION;

    @JkDoc("The unique application id within the organization. By default, it values to the root dir name.")
    public String appId = getBaseDir().toAbsolutePath().getFileName().toString();

    @JkDoc("Project version injected by CI/CD tool")
    @JkInjectProperty("PROJECT_VERSION")
    private String projectVersion;

    private JkProject project = JkProject.of();

    @Override
    protected void init() {
        project.setModuleId(appId);
        project.setVersion(projectVersion);
        project.packaging.getManifest().addMainAttribute(JkManifest.IMPLEMENTATION_VERSION, projectVersion);
        project.testing.testSelection.addExcludePatterns(E2E_TEST_PATTERN);

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
    }

    @JkDoc("Performs a simple build, without code coverage")
    public void pack() {
        project.clean().pack();
    }

    @JkDoc("Performs a build including quality static analysis.")
    public void packQuality() {
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
        project.prepareRunJar(JkProject.RuntimeDeps.EXCLUDE).run();
    }

    @JkDoc("Displays the dependency tree on the console.")
    public void depTree() {
        project.displayDependencyTree();
    }

    @JkDoc("Run the Docker image and execute E2E tests (browser based)")
    public void runE2e() {
        new DockerTester().run();
    }

    @Override
    public JkIdeSupport getJavaIdeSupport() {
        return project.getJavaIdeSupport();
    }

    private Path reactBaseDir() {
        return getBaseDir().resolve(REACTJS_BASE_DIR);
    }

    private String imageName() {
        String version = Optional.ofNullable(projectVersion).orElse("latest");
        return "my-org/" + appId + ":" + version;
    }


    private JkSonarqube sonarqubeBase() {
        return JkSonarqube.ofVersion(getRunbase().getDependencyResolver(), SONARQUBE_VERSION);
    }

    private void execSelenideTests(String baseUrl) {
        JkTestSelection selection = project.testing.createDefaultTestSelection()
                .addIncludePatterns(E2E_TEST_PATTERN);
        JkTestProcessor testProcessor = project.testing.createDefaultTestProcessor().setForkingProcess(true);
        testProcessor.getForkingProcess()
                .setLogWithJekaDecorator(true)
                .setLogCommand(true)
                .addJavaOptions("-Dselenide.reportsFolder=jeka-output/test-report/selenide")
                .addJavaOptions("-Dselenide.downloadsFolder=jeka-output/test-report/selenide-download")
                .addJavaOptions("-Dselenide.headless=true")
                .addJavaOptions("-Dselenide.baseUrl=" + baseUrl);
        testProcessor.launch(project.testing.getTestClasspath(), selection).assertSuccess();
    }

    class DockerTester extends JkApplicationTester {

        int port;

        String baseUrl;

        String containerName;

        @Override
        protected void startApp() {
            port = findFreePort();
            baseUrl = "http://localhost:" + port;
            containerName = project.getBaseDir().toAbsolutePath().getFileName().toString() + "-" + port;
            JkDocker.of()
                    .addParams("run", "-d", "-p", String.format("%s:8080", port), "--name",
                            containerName, SpringBootTemplateBuild.this.imageName())
                    .setInheritIO(false)
                    .setLogWithJekaDecorator(true)
                    .exec();
        }

        @Override
        protected boolean isApplicationReady() {
            return JkUtilsNet.isAvailableAndOk(baseUrl, JkLog.isDebug());
        }

        @Override
        protected void executeTests() {
            execSelenideTests(baseUrl);
        }

        @Override
        protected void stopGracefully() {
            JkDocker.of()
                    .addParams("rm", "-f", containerName)
                    .setInheritIO(false)
                    .setLogWithJekaDecorator(true)
                    .exec();
        }
    }

}
