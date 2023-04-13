package com.dotcms.plugin.app;

public enum AppKeys {
    ACTIVE_KEY("active"),
    ROLE_WHITE_LIST_KEY("roleWhiteList");

    final public String key;

    AppKeys(String key) {
        this.key = key;
    }


    public final static String APP_KEY = "DotUserRegistration";

    public final static String APP_YAML_NAME = APP_KEY + ".yml";

}
