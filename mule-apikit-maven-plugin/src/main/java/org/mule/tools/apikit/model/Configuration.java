package org.mule.tools.apikit.model;

import java.util.Map;

public class Configuration {

    private String environment;
    private Map<String, String> properties;

    public Configuration(){}

    public Configuration(String environment, Map<String, String> properties) {
        this.environment = environment;
        this.properties = properties;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
