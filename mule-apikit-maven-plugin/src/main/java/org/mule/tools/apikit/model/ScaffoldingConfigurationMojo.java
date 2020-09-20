package org.mule.tools.apikit.model;

public class ScaffoldingConfigurationMojo {
    private boolean showConsole;
    private String externalCommonFile;
    private String apiId;

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
}
