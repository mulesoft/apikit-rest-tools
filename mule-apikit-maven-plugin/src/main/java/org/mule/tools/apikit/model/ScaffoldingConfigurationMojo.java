package org.mule.tools.apikit.model;

import java.util.Map;

public class ScaffoldingConfigurationMojo {
    private boolean showConsole;
    private String externalCommonFile;
    private String apiId;
    private Properties properties;

    public ScaffoldingConfigurationMojo() {
        this.showConsole = true;
        this.externalCommonFile = null;
        this.apiId = null;
    }

    public ScaffoldingConfigurationMojo(boolean showConsole, String externalCommonFile, String apiId) {
        this.showConsole = showConsole;
        this.externalCommonFile = externalCommonFile;
        this.apiId = apiId;
    }

    public boolean isShowConsole() {
        return showConsole;
    }

    public void setShowConsole(boolean showConsole) {
        this.showConsole = showConsole;
    }

    public String getExternalCommonFile() {
        return externalCommonFile;
    }

    public void setExternalCommonFile(String externalCommonFile) {
        this.externalCommonFile = externalCommonFile;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
