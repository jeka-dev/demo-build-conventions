package dev.jeka.demo.templates.springboot.reactjs;

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.testing.JkApplicationTester;
import dev.jeka.core.api.testing.JkTestProcessor;
import dev.jeka.core.api.testing.JkTestSelection;
import dev.jeka.core.api.tooling.docker.JkDocker;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.api.utils.JkUtilsNet;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkException;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;
import dev.jeka.plugins.nodejs.JkNodeJsProject;
import dev.jeka.plugins.nodejs.NodeJsKBean;
import dev.jeka.plugins.sonarqube.JkSonarqube;
import dev.jeka.plugins.sonarqube.SonarqubeKBean;
import dev.jeka.plugins.springboot.SpringbootKBean;

import java.nio.file.Files;
import java.nio.file.Path;

@JkDoc("""
        Builds a Spring-Boot project, optionally containing a reactjs frontend.
        This build handles Java compilation, Junit testing with coverage, reactjs build, Sonarqube analysis.
        
        This template is voluntary designed to be rigid for enforcing conventions.
        Only applicationId and nodeJs version can be overridden by users.
        
        The project version, along the SonarQube host/token props, are expected to be injected by the CI tool.
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

    private final JkProject project = load(ProjectKBean.class).project;

    private JkNodeJsProject nodeJsProject;

    @JkDoc("""
           - Loads `springboot` KBeans
           - Loads and configures `nodeJs` KBeans if 'reactjs-client' dir exists
           """)
    @Override
    protected void init() {
        project.setModuleId(appId);
        JkVersionFromGit.of(getBaseDir(), "").handleVersioning(project);
        project.testing.testSelection.addExcludePatterns(E2E_TEST_PATTERN);

        // Load springboot
        load(SpringbootKBean.class);

        // Build reactJs if 'reactjs-client' dir is present. The bundled js app is copied in 'resources/static'.
        Path jsBaseDir = getBaseDir().resolve(REACTJS_BASE_DIR);
        if (Files.exists(jsBaseDir)) {
            JkLog.verbose("JS project detected in %s.", REACTJS_BASE_DIR);
            NodeJsKBean nodeJsKBean = load(NodeJsKBean.class);
            this.nodeJsProject = nodeJsKBean.configureProject()
                    .setBaseJsDir(jsBaseDir)
                    .setBuildDir("build")
                    .setBuildCommands("npx yarn install ", "npm run build")
                    .setCopyToResourcesPackAction(project, "static");
        } else {
            JkLog.verbose("No JS project detected in %.", REACTJS_BASE_DIR);
        }
    }

    @JkDoc("Runs Sonarqube analysis on both Java and Javascript")
    public void sonar() {

        SonarqubeKBean sonarqubeKBean = load(SonarqubeKBean.class);

        // Run sonarqube on Jaka code
        sonarqubeKBean.sonarqube.run();

        // Run sonarqube on JS project
        if (nodeJsProject != null) {
            sonarqubeKBean.sonarqube
                    .setProperty(JkSonarqube.PROJECT_KEY, project.getModuleId()+ "-js")
                    .setProperty(JkSonarqube.PROJECT_VERSION, project.getVersion().getValue())
                    .setProperty(JkSonarqube.LANGUAGE, "javascript")
                    .setProperty(JkSonarqube.SOURCES, nodeJsProject.getBaseJsDir().toString())
                    .setProperty("exclusions", "node_modules")
                    .setProperties(getRunbase().getProperties())
                    .run();
        }
    }

    @JkDoc("Executes end-to-end tests")
    public void e2e() {
        new DockerTester().run();
    }

    class DockerTester extends JkApplicationTester {

        int port;

        String baseUrl;

        String containerName;

        @Override
        protected void startApp() {
            var imageName = load(DockerKBean.class).resolveJvmImageName();
            if (!JkDocker.of().setLogWithJekaDecorator(false).assertPresent().getLocalImages().contains(imageName)) {
                throw new JkException("Image %s not found in Docker registry. Build it first, " +
                        "e.g., 'jeka docker: build'.", imageName);
            }
            port = findFreePort();
            baseUrl = "http://localhost:" + port;
            containerName = project.getBaseDir().toAbsolutePath().getFileName().toString() + "-" + port;
            JkDocker.of()
                    .assertPresent()
                    .addParams("run", "-d", "-p", String.format("%s:8080", port), "--name",
                            containerName, imageName)
                    .setInheritIO(false)
                    .setLogWithJekaDecorator(JkLog.isVerbose())
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
                    .setLogWithJekaDecorator(JkLog.isVerbose())
                    .exec();
        }
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

}
