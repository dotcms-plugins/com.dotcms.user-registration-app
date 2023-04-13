package com.dotcms.plugin.app;

public enum ActiveType {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ONLY_ADMIN("onlyadmin");

    final public String type;

    ActiveType(String type) {
        this.type = type;
    }

    public static ActiveType valueOfType(String type) {

        for (final ActiveType activeType : values()) {
            if (activeType.type.equals(type)) {
                return activeType;
            }
        }

        return INACTIVE;
    }
}
