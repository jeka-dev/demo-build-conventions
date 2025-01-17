package _dev;

import dev.jeka.core.api.depmanagement.JkVersion;
import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.tool.JkJekaVersionRanges;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class Build extends KBean {

    final BaseKBean baseKBean = load(BaseKBean.class);

    final MavenKBean mavenKBean = load(MavenKBean.class);

    protected void init() {

        // Plugin version compatibility
        JkJekaVersionRanges.setCompatibilityRange(baseKBean.getManifest(),
                JkInfo.getJekaVersion(),
                "https://raw.githubusercontent.com/jeka-dev/template-examples/master/breaking_versions.txt");

        // Those dependencies should not be included as transitive dependencies
        mavenKBean.getMavenPublication().customizeDependencies(deps -> deps
                .minus("dev.jeka:jacoco-plugin")
                .minus("dev.jeka:sonarqube-plugin")
                .minus("dev.jeka:springboot-plugin")
                .minus("dev.jeka:nodejs-plugin"));
    }

}