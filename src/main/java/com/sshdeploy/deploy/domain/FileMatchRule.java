package com.sshdeploy.deploy.domain;

import java.util.UUID;

/** User-defined file match rule; built-in templates are defined separately and not persisted. */
public final class FileMatchRule {
    private String id = UUID.randomUUID().toString();
    private String name = "";
    /** Regular expression applied to file names under the upload directory. */
    private String pattern = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
