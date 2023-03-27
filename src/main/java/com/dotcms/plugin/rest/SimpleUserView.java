package com.dotcms.plugin.rest;

import java.io.Serializable;

public class SimpleUserView implements Serializable {

    private String firtName;
    private String lastName;
    private String email;

    public SimpleUserView(String firtName, String lastName, String email) {
        this.firtName = firtName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getFirtName() {
        return firtName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }
}
