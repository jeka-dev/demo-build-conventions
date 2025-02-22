package dev.jeka.demo.templates.springboot.reactjs;

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.plugins.nodejs.NodeJsKBean;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;
import dev.jeka.plugins.springboot.SpringbootKBean;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDoc("""
        Builds a Spring-Boot project, optionally containing a reactjs frontend.
        This build handles:
          - Springboot build, including unit-tests and bootable jar creation.
          - React-Js build and tests (if the project includes a react app)
          - Sonarqube analysis for both JS and Java project, including test coverage via Jacoco
          - End-to-end testing against app deployed by Docker, using Selenium IDE
        
        To launch e2e tests, execute `jeka template: e2e`. This launches automatically the springboot
        application on docker, and execute SeleniumIDE tests located in `e2e` package from *src/test/java*
        
        To launch sonarqube analysis, execute `jeka template: sonarqube`. This will launch 1 analysis 
        on the Java project, and an another one on the JS project.
        
        To use this template, just copy paste the following snippet in your *jeka.properties* file:
        ```properties
        jeka.inject.classpath=dev.jeka:template-examples:[version]
        @template=on
        ```
        """)
@JkDep("dev.jeka:nodejs-plugin")
@JkDep("dev.jeka:sonarqube-plugin")
@JkDep("dev.jeka:jacoco-plugin")
@JkDep("dev.jeka:springboot-plugin")
public class Template extends KBean {

    private static final String REACTJS_BASE_DIR = "reactjs-app";

    private static final String E2E_TEST_PATTERN = "^e2e\\..*";

    @JkDoc("The unique application id within the organization. By default, it values to the root dir name.")
    public String appId = getBaseDir().toAbsolutePath().getFileName().toString();

    @JkDoc("If true, end-to-end tests will be run against the application deployed on docker")
    public boolean e2eTestOnDocker = false;

    // Compute if the NodeJs KBean is required for running the build, according the presence of 'react-js' dir.
    @JkRequire
    private static Class<?> requireNodeJs(JkRunbase runbase) {
        Path jsBaseDir = runbase.getBaseDir().resolve(REACTJS_BASE_DIR);
        if (Files.exists(jsBaseDir)) {
            JkLog.verbose("JS project detected in %s.", REACTJS_BASE_DIR);
            return NodeJsKBean.class;
        } else {
            JkLog.verbose("No JS project detected in %.", REACTJS_BASE_DIR);
            return null;
        }
    }

    @JkPreInit
    private static void preInit(ProjectKBean projectKBean) {
        projectKBean.gitVersioning.enable = true;
    }

    @JkPreInit
    private static void preInit(NodeJsKBean nodeJsKBean) {
        nodeJsKBean.appDir = REACTJS_BASE_DIR;
        nodeJsKBean.buildDir = "build";
        nodeJsKBean.buildCmd = "npx yarn install, npm run build";
        nodeJsKBean.targetResourceDir = "static";
        nodeJsKBean.configureProject = true;
    }

    @JkPostInit(required = true) // just to declare that springboot KBean is required
    private void postInit(SpringbootKBean springbootKBean) {
    }

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.setModuleId(appId)
                .testing.testSelection.addExcludePatterns(E2E_TEST_PATTERN);
        project.addE2eTester("localhost-tester", this.e2eTestOnDocker ? this::e2eDockerTest : this::e2eTest);
        project.addQualityChecker("sonarqube-js", this::runSonarqubeJs);
    }

    private void e2eTest() {
        load(SpringbootKBean.class).createE2eAppTester(this::execSelenideTests)
                .setShowAppLogs(JkLog.isVerbose())
                .run();
    }

    private void e2eDockerTest() {
        load(DockerKBean.class).createJvmAppTester(this::execSelenideTests)
                .setShowAppLogs(JkLog.isVerbose())
                .run();
    }

    private void e2eDockerNative() {
        load(DockerKBean.class).createNativeAppTester(this::execSelenideTests)
                .setShowAppLogs(true)
                .run();
    }

    private void runSonarqubeJs() {
        if (find(NodeJsKBean.class).isPresent()) {
            NodeJsKBean nodeJsKBean = find(NodeJsKBean.class).get();
            JkProject project = load(ProjectKBean.class).project;
            SonarqubeKBean sonarqubeKBean = load(SonarqubeKBean.class);
            sonarqubeKBean.getSonarqube()
                    .setProperty(JkSonarqube.PROJECT_KEY, project.getModuleId()+ "-js")
                    .setProperty(JkSonarqube.PROJECT_VERSION, project.getVersion().getValue())
                    .setProperty(JkSonarqube.LANGUAGE, "javascript")
                    .setProperty(JkSonarqube.SOURCES, nodeJsKBean.getNodeJsProject().getBaseJsDir().toString())
                    .setProperty("exclusions", "node_modules")
                    .setProperties(getRunbase().getProperties())
                    .run();
            if (sonarqubeKBean.gate) {
                sonarqubeKBean.getSonarqube().checkQualityGate();
            }
        }
    }

    private void execSelenideTests(String baseUrl) {
        JkProject project = load(ProjectKBean.class).project;
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

}
