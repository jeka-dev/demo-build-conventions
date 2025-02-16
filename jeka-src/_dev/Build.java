package _dev;

import dev.jeka.core.api.system.JkInfo;
import dev.jeka.core.tool.JkJekaVersionRanges;
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class Build extends KBean {

    @JkPostInit(required = true)
    private void postInit(BaseKBean baseKBean) {

        // Plugin version compatibility
        JkJekaVersionRanges.setCompatibilityRange(baseKBean.getManifest(),
                JkInfo.getJekaVersion(),
                "https://raw.githubusercontent.com/jeka-dev/template-examples/master/breaking_versions.txt");
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {

    }

}