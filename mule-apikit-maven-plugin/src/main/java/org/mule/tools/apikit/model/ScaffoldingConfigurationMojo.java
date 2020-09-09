package org.mule.tools.apikit.model;

public class ScaffoldingConfigurationMojo {
    private boolean showConsole;
    private String externalCommonFile;

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
}
