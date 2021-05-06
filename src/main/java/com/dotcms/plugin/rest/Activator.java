package com.dotcms.plugin.rest;

import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Logger;
import org.osgi.framework.BundleContext;

public class Activator extends GenericBundleActivator {

	Class clazz = UserResource.class;

	public void start(BundleContext context) throws Exception {

		Logger.info(this.getClass(), "Adding new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.addResource(clazz);
	}

	public void stop(BundleContext context) throws Exception {

		Logger.info(this.getClass(), "Removing new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.removeResource(clazz);
	}

}
