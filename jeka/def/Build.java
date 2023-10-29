import dev.jeka.core.api.crypto.gpg.JkGpg;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.java.JkJavaVersion;
import dev.jeka.core.api.project.JkProject;
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
        project.setJvmTargetVersion(JkJavaVersion.V8);
        project.compilation.configureDependencies(deps -> deps
                .andFiles(JkLocator.getJekaJarPath())
                .and("dev.jeka:nodejs-plugin:" + JkInfo.getJekaVersion())
                .and("dev.jeka:sonarqube-plugin:" + JkInfo.getJekaVersion())
                .and("dev.jeka:jacoco-plugin:" + JkInfo.getJekaVersion())
                .and("dev.jeka:springboot-plugin:" + JkInfo.getJekaVersion())
        );

        JkJekaVersionCompatibilityChecker.setCompatibilityRange(project.packaging.manifest,
                "0.10.35",
                "https://raw.githubusercontent.com/your_org/your_repo/master/breaking_versions.txt");

        // This section is necessary to publish on a public repository
        project.publication
                .setModuleId("org.github.djeang:jeka-build-templates")
                .setVersion(() -> JkGit.of().getVersionFromTag())
                .setRepos(JkRepoSet.ofOssrhSnapshotAndRelease(ossrhUser, ossrhPwd, JkGpg.ofDefaultGnuPg().getSigner("")))
                .maven
                    .pomMetadata
                        .addApache2License()
                        .setProjectName("Collection of build templates for JeKa")
                        .setProjectDescription("Provides opinionated KBeans for building projects with minimal typing.")
                        .setProjectUrl("https://github.com/djeang/jeka-build-templates")
                        .setScmUrl("https://github.com/djeang/jeka-build-templates");
    }

}