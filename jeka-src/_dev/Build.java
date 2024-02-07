package _dev;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.JkJekaVersionCompatibilityChecker;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.self.SelfKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

@JkInjectClasspath("dev.jeka:nodejs-plugin")
@JkInjectClasspath("dev.jeka:sonarqube-plugin")
@JkInjectClasspath("dev.jeka:jacoco-plugin")
@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends KBean {

    private final SelfKBean selfKBean = load(SelfKBean.class);


    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    public boolean publishOnMavenCentral;

    @Override
    protected void init() {

        selfKBean.setModuleId("dev.jeka:template-examples");

        String jekaVersion =  JkInfo.getJekaVersion();
        selfKBean.manifestCustomizers.add(manifest ->
            JkJekaVersionCompatibilityChecker.setCompatibilityRange(
                    manifest,
                    jekaVersion,
                    "https://raw.githubusercontent.com/jeka-dev/template-examples/master/breaking_versions.txt")
        );

        // Handle version with git
        JkVersionFromGit.of().handleVersioning(selfKBean);

        // Configure Maven Publication
        JkMavenPublication mavenPublication = load(MavenKBean.class).getMavenPublication();
        mavenPublication
                .pomMetadata
                .addApache2License()
                .addGithubDeveloper("djeangdev", "djeangdev@yahoo.fr")
                .setProjectName("Collection of build templates for JeKa")
                .setProjectDescription("Provides opinionated KBeans for building projects with minimal typing.")
                .setProjectUrl("https://github.com/jeka-dev/template-examples")
                .setScmUrl("https://github.com/jeka-dev/template-examples.git");

        // Set required information to be published on Maven Central
        if (publishOnMavenCentral) {
            JkFileSigner fileSigner = JkGpgSigner.ofStandardProject(this.getBaseDir());
            mavenPublication.setRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, fileSigner));
            JkNexusRepos.handleAutoRelease(mavenPublication);
        }

    }

}