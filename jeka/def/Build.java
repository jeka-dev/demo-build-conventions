import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.publication.JkNexusRepos;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.project.JkProjectCompilation;
import dev.jeka.core.api.system.JkIndentLogDecorator;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.tooling.JkGit;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectProperty;
import dev.jeka.core.tool.JkJekaVersionCompatibilityChecker;
import dev.jeka.core.tool.builtins.project.ProjectJkBean;

class Build extends JkBean {

    final ProjectJkBean projectBean = getBean(ProjectJkBean.class).lately(this::configure);

    @JkInjectProperty("OSSRH_USER")
    public String ossrhUser;  // OSSRH user and password will be injected from environment variables

    @JkInjectProperty("OSSRH_PWD")
    public String ossrhPwd;

    private void configure(JkProject project) {
        project.setJvmTargetVersion(JkJavaVersion.V17);
        String jekaVersion =  JkInfo.getJekaVersion();
        project.compilation.configureDependencies(deps -> deps
                .andFiles(JkLocator.getJekaJarPath())
                .and("dev.jeka:nodejs-plugin:" + jekaVersion)
                .and("dev.jeka:sonarqube-plugin:" + jekaVersion)
                .and("dev.jeka:jacoco-plugin:" + jekaVersion)
                .and("dev.jeka:springboot-plugin:" + jekaVersion)
        );


        JkJekaVersionCompatibilityChecker.setCompatibilityRange(project.packaging.manifest,
                jekaVersion,
                "https://raw.githubusercontent.com/jeka-dev/template-examples/master/breaking_versions.txt");

        JkGpg gpg = JkGpg.ofStandardProject(this.getBaseDir());
        project.publication
                .setModuleId("dev.jeka:template-examples")
                .setVersion(() -> JkGit.of().getVersionFromTag())
                .setRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, gpg.getSigner("")))
                .maven
                    .pomMetadata
                        .addApache2License()
                        .addGithubDeveloper("djeangdev", "djeangdev@yahoo.fr")
                        .setProjectName("Collection of build templates for JeKa")
                        .setProjectDescription("Provides opinionated KBeans for building projects with minimal typing.")
                        .setProjectUrl("https://github.com/jeka-dev/template-examples")
                        .setScmUrl("https://github.com/jeka-dev/template-examples.git");
        JkNexusRepos.handleAutoRelease(project);
    }

}