package com.dotcms.plugin.view;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.apache.velocity.tools.view.tools.ViewTool;

import java.util.List;

public class UsersViewTool implements ViewTool {

    @Override
    public void init(Object initData) {
    }

    public List<User> getUsers() {

        List<User> users = null;

        try {

            users = APILocator.getUserAPI().findAllUsers();
        } catch (DotDataException e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return users;
    }
}
