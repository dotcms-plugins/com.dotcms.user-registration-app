package com.dotcms.plugin.view;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class UsersViewToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "myusers";
    }

    @Override
    public String getScope () {
        return ViewContext.APPLICATION;
    }

    @Override
    public String getClassname () {
        return UsersViewTool.class.getName();
    }

    @Override
    public Object getInstance (final Object initData) {

        final UsersViewTool viewTool = new UsersViewTool();
        viewTool.init(initData);

        setScope(ViewContext.APPLICATION);

        return viewTool;
    }
}
