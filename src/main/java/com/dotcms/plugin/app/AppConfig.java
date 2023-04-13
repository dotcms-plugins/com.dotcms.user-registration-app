package com.dotcms.plugin.app;

import java.io.Serializable;
import java.util.Set;

public class AppConfig implements Serializable {

    private final Set<String> rolesWhiteList;
    private final ActiveType activeType;

    public AppConfig(Set<String> rolesWhiteList, ActiveType activeType) {
        this.rolesWhiteList = rolesWhiteList;
        this.activeType = activeType;
    }

    public Set<String> getRolesWhiteList() {
        return rolesWhiteList;
    }

    public ActiveType getActiveType() {
        return activeType;
    }
}
