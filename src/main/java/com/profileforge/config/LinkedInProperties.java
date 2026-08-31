package com.profileforge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "linkedin")
public class LinkedInProperties {
    private String liAt;
    private String jsessionid;

    public String getLiAt() {
        return liAt;
    }

    public void setLiAt(String liAt) {
        this.liAt = liAt;
    }

    public String getJsessionid() {
        return jsessionid;
    }

    public void setJsessionid(String jsessionid) {
        this.jsessionid = jsessionid;
    }
}
