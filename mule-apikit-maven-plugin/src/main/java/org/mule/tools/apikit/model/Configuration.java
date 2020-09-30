package org.mule.tools.apikit.model;

import java.util.Map;

public class Configuration {

    private String environment;
    private CommonProperties commonProperties;
    private Map<String, Object> properties;

    public Configuration(){}

    public Configuration(String environment, CommonProperties commonProperties, Map<String, Object> properties) {
        this.environment = environment;
        this.commonProperties = commonProperties;
        this.properties = properties;
    }

    public CommonProperties getCommonProperties() {
        return commonProperties;
    }

    public void setCommonProperties(CommonProperties commonProperties) {
        this.commonProperties = commonProperties;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
