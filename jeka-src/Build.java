import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.JkJekaVersionRanges;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.plugins.nexus.JkNexusRepos;


class Build extends KBean {

    final JkProject project = load(ProjectKBean.class).project;

    final MavenKBean mavenKBean = load(MavenKBean.class);

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    protected void init() {
        String jekaVersion =  JkInfo.getJekaVersion();

        // source layout
        project.flatFacade()
                .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
                .mixResourcesAndSources();

        project.compilation.customizeDependencies(deps -> deps
                .and("dev.jeka:jeka-core:"  + jekaVersion)
                .and("dev.jeka:nodejs-plugin:" + jekaVersion)
                .and("dev.jeka:sonarqube-plugin:" + jekaVersion)
                .and("dev.jeka:jacoco-plugin:" + jekaVersion)
                .and("dev.jeka:springboot-plugin:" + jekaVersion)
        );
        JkJekaVersionRanges.setCompatibilityRange(project.packaging.getManifest(),
                jekaVersion,
                "https://raw.githubusercontent.com/jeka-dev/template-examples/master/breaking_versions.txt");

        // Set required information to be published on Maven Central
        JkGpgSigner gpgSigner = JkGpgSigner.ofStandardProject(this.getBaseDir());
        JkRepoSet repos = JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, gpgSigner);

        mavenKBean.getMavenPublication()
                .setModuleId("dev.jeka:template-examples")
                .setRepos(repos)
                .pomMetadata
                    .addApache2License()
                    .addGithubDeveloper("djeangdev", "djeangdev@yahoo.fr")
                    .setProjectName("Collection of build templates for JeKa")
                    .setProjectDescription("Provides opinionated KBeans for building projects with minimal typing.")
                    .setProjectUrl("https://github.com/jeka-dev/template-examples")
                    .setScmUrl("https://github.com/jeka-dev/template-examples.git");

        JkNexusRepos.handleAutoRelease(mavenKBean.getMavenPublication());
        JkVersionFromGit.of().handleVersioning(project);
    }

}