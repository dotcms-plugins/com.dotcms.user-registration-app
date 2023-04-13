package com.dotcms.plugin.rest;

import com.dotcms.plugin.actionlet.CreateUserActionlet;
import com.dotcms.plugin.app.AppKeys;
import com.dotcms.plugin.view.UsersViewToolInfo;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class Activator extends GenericBundleActivator {

	Class clazz = MyUserResource.class;

	private final File installedAppYaml = new File(ConfigUtils.getAbsoluteAssetsRootPath() + File.separator + "server"
			+ File.separator + "apps" + File.separator + AppKeys.APP_YAML_NAME);


	public void start(BundleContext context) throws Exception {

		//Initializing services...
		initializeServices( context );
		Logger.info(this.getClass(), "Adding new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.addResource(clazz);
		Logger.info(this.getClass(), "Adding new ViewTool :" + UsersViewToolInfo.class.getSimpleName());
		registerViewToolService(context, new UsersViewToolInfo());
		Logger.info(this.getClass(), "Adding new Actionlet :" + CreateUserActionlet.class.getSimpleName());
		registerActionlet( context, new CreateUserActionlet() );
		// copy the yaml
		copyAppYml();
	}

	public void stop(BundleContext context) throws Exception {

		deleteYml();
		Logger.info(this.getClass(), "Removing new Restful Service:" + clazz.getSimpleName());
		RestServiceUtil.removeResource(clazz);
		Logger.info(this.getClass(), "Removing ViewTool :" + UsersViewToolInfo.class.getSimpleName());
		unregisterViewToolServices();

		//Unregister all the bundle services
		unregisterServices(context);
	}

	/**
	 * copies the App yaml to the apps directory and refreshes the apps
	 *
	 * @throws IOException
	 */
	private void copyAppYml() throws IOException {

		Logger.info(this.getClass().getName(), "copying YAML File:" + installedAppYaml);

		if (!installedAppYaml.exists()) {

			installedAppYaml.createNewFile();
		}

		try (final InputStream in = this.getClass().getResourceAsStream("/" + AppKeys.APP_YAML_NAME)) {

			IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));
		}

		CacheLocator.getAppsCache().clearCache();
	}

	/**
	 * Deletes the App yaml to the apps directory and refreshes the apps
	 *
	 * @throws IOException
	 */
	private void deleteYml() throws IOException {

		Logger.info(this.getClass().getName(), "deleting the YAML File:" + installedAppYaml);

		installedAppYaml.delete();
		CacheLocator.getAppsCache().clearCache();
	}

}
