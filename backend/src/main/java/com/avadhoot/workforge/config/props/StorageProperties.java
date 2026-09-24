package com.avadhoot.workforge.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workforge.storage")
public class StorageProperties {

    private String location = "./data/attachments";

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
