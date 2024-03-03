package _dev;

import dev.jeka.core.api.crypto.JkFileSigner;
import dev.jeka.core.api.crypto.gpg.JkGpgSigner;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.tooling.git.JkVersionFromGit;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.JkJekaVersionRanges;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;


class Build extends KBean {

    public static final String VERSION_BREAK_URL =
            "https://raw.githubusercontent.com/jeka-dev/template-examples/master/breaking_versions.txt";

    private final BaseKBean baseKBean = load(BaseKBean.class);


    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    public boolean publishOnMavenCentral;

    @Override
    protected void init() {

        baseKBean.setModuleId("dev.jeka:template-examples");

        // Include version range in manifest
        baseKBean.manifestCustomizers.add(
                JkJekaVersionRanges.manifestCustomizer(JkInfo.getJekaVersion(), VERSION_BREAK_URL));

        // Handle version with git
        JkVersionFromGit.of().handleVersioning(baseKBean);

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