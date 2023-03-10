package com.dotcms.plugin.rest;

import com.dotcms.plugin.view.UsersViewToolInfo;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Logger;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

	Class clazz = MyUserResource.class;

	public void start(BundleContext context) throws Exception {

		Logger.info(this.getClass(), "Adding new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.addResource(clazz);
		Logger.info(this.getClass(), "Adding new ViewTool :" + UsersViewToolInfo.class.getSimpleName());
		registerViewToolService(context, new UsersViewToolInfo());
	}

	public void stop(BundleContext context) throws Exception {

		Logger.info(this.getClass(), "Removing new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.removeResource(clazz);
		Logger.info(this.getClass(), "Removing ViewTool :" + UsersViewToolInfo.class.getSimpleName());
		unregisterViewToolServices();
	}

}
