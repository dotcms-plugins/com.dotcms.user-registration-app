package com.dotcms.plugin.rest;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.quartz.job.DeleteUserJob;
import com.dotmarketing.util.*;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.business.ajax.DwrUtil.getLoggedInUser;
import static com.dotmarketing.business.ajax.DwrUtil.validateUsersPortletPermissions;

@Path("/v1/users")
public class UserResource {

    private final WebResource webResource = new WebResource();
	private final UserAPI userAPI;
	private final RoleAPI roleAPI;

	public UserResource() {
		this.userAPI = APILocator.getUserAPI();
		this.roleAPI = APILocator.getRoleAPI();
	}

	/**
	 * Creates an user.
	 * If userId is sent will be use, if not will be created "userId-" + UUIDUtil.uuid().
	 * By default, users will be inactive unless the active = true is sent and user has permissions( is Admin or access
	 * to Users and Roles portlets).
	 * FirstName, LastName, Email and Password are required.
	 *
	 *
	 * Scenarios:
	 *  1. No Auth or User doing the request do not have access to Users and Roles Portlets
	 *  	- Always will be inactive
	 *  	- Only the	Role DOTCMS_FRONT_END_USER will be added
	 *  2. Auth, User is Admin or have access to Users and Roles Portlets
	 *  	- Can be active if JSON includes ("active": true)
	 *  	- The list of RoleKey will be use to assign the roles, if the roleKey doesn't exist will be
	 *  		created under the ROOT ROLE.
	 *
	 * @param httpServletRequest
	 * @param createUserForm
	 * @return User Created
	 * @throws Exception
	 */
	@POST
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response create(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 final CreateUserForm createUserForm) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.requiredAnonAccess(AnonymousAccess.WRITE)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() || (APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), modUser) &&
				APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), modUser));
		final User userToUpdated = this.createNewUser(null == modUser? APILocator.systemUser(): modUser,
				isRoleAdministrator, createUserForm);

		return Response.ok(new ResponseEntityView(map("userID", userToUpdated.getUserId(),
				 "user", userToUpdated.toMap()))).build(); // 200
	} // create.

	/**
	 * Deles an user.
	 * Receives the user id to delete and an user to replace
	 * The replacing user id will be used to set as a reference on all contents that the user to delete
	 * previously own.
	 * This may be a heavy operation, so it is execute in background, the endpoint returns true but it does not
	 * mean the user is actually deleted, since the replacement of content is happening in background and may take a while.
	 *
	 * @param httpServletRequest
	 * @param createUserForm
	 * @return true if the
	 * @throws Exception
	 */
	@GET
	@Path("/id/{userId}/_isDeleteInProgress")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response isDeleteInProgress(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 @PathParam("userId") final String userId) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.requiredAnonAccess(AnonymousAccess.WRITE)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() || (APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), modUser) &&
				APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), modUser));

		final User freshUser = userAPI.loadUserById(userId);
		return Response.ok(new ResponseEntityView(freshUser.isDeleteInProgress())).build(); // 200
	} // isDeleteInProgress.

	/**
	 * Deles an user.
	 * Receives the user id to delete and an user to replace
	 * The replacing user id will be used to set as a reference on all contents that the user to delete
	 * previously own.
	 * This may be a heavy operation, so it is execute in background, the endpoint returns true but it does not
	 * mean the user is actually deleted, since the replacement of content is happening in background and may take a while.
	 *
	 * @param httpServletRequest
	 * @param createUserForm
	 * @return true if the
	 * @throws Exception
	 */
	@DELETE
	@Path("/id/{userId}/replacing/{replacingUserId}")
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response delete(@Context final HttpServletRequest httpServletRequest,
								 @Context final HttpServletResponse httpServletResponse,
								 @PathParam("userId") final String userId,
								 @PathParam("replacingUserId") final String replacingUserId) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requestAndResponse(httpServletRequest, httpServletResponse)
				.requiredAnonAccess(AnonymousAccess.WRITE)
				.init().getUser();

		final boolean isRoleAdministrator = modUser.isAdmin() || (APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), modUser) &&
				APILocator.getLayoutAPI().doesUserHaveAccessToPortlet(PortletID.USERS.toString(), modUser));

		String date = DateUtil.getCurrentDate();

		//Validate if this logged in user has the required permissions to access the users portlet
		ActivityLogger.logInfo(getClass(), "Deleting User", "Date: " + date + "; "+ "User:" + userId+"; Replacing entries with User:"+replacingUserId);
		AdminLogger.log(getClass(), "Deleting User", "Date: " + date + "; "+ "User:" + userId+"; Replacing entries with User:"+replacingUserId);

		try {

			final User userToDelete    = this.userAPI.loadUserById(userId,modUser,false);
			final User replacementUser = this.userAPI.loadUserById(replacingUserId, modUser,false);
			DeleteUserJob.triggerDeleteUserJob(userToDelete, replacementUser, modUser,
					PageMode.get(httpServletRequest).respectAnonPerms);
		} catch(DotStateException e) {

			ActivityLogger.logInfo(getClass(), "Error Deleting User", "Date: " + date + ";  "+ "User:" + userId);
			AdminLogger.log(getClass(), "Error Deleting User", "Date: " + date + ";  "+ "User:" + userId);
			throw e;
		}

		ActivityLogger.logInfo(getClass(), "User Deleted", "Date: " + date + "; "+ "User:" + userId+"; Replaced entries with User:"+replacingUserId);
		AdminLogger.log(getClass(), "User Deleted", "Date: " + date + "; "+ "User:" + userId+"; Replaced entries with User:"+replacingUserId);

		return Response.ok(new ResponseEntityView(true)).build(); // 200
	} // delete.

	protected User createNewUser(final User modUser, final boolean isRoleAdministrator,
								 final CreateUserForm createUserForm) throws DotDataException, DotSecurityException, ParseException {

		final String userId = UtilMethods.isSet(createUserForm.getUserId())?createUserForm.getUserId(): "userId-" + UUIDUtil.uuid();
		final User user = this.userAPI.createUser(userId, createUserForm.getEmail());

		user.setFirstName(createUserForm.getFirstName());

		if (UtilMethods.isSet(createUserForm.getLastName())) {
			user.setLastName(createUserForm.getLastName());
		}

		if (UtilMethods.isSet(createUserForm.getBirthday())) {
			user.setBirthday(DateUtil.parseISO(createUserForm.getBirthday()));
		}

		if (UtilMethods.isSet(createUserForm.getMiddleName())) {
			user.setMiddleName(createUserForm.getMiddleName());
		}

		if (createUserForm.getLanguageId() <= 0) {
			user.setLanguageId(String.valueOf(createUserForm.getLanguageId() <= 0?
					APILocator.getLanguageAPI().getDefaultLanguage().getId(): createUserForm.getLanguageId()));
		}

		if (UtilMethods.isSet(createUserForm.getNickName())) {
			user.setNickName(createUserForm.getNickName());
		}

		if (UtilMethods.isSet(createUserForm.getTimeZoneId())) {
			user.setTimeZoneId(createUserForm.getTimeZoneId());
		}

		user.setPassword(new String(createUserForm.getPassword()));
		user.setMale(createUserForm.isMale());
		user.setCreateDate(new Date());

		if (UtilMethods.isSet(createUserForm.getAdditionalInfo())) {
			user.setAdditionalInfo(createUserForm.getAdditionalInfo());
		}

		List<String> roleKeys = list(Role.DOTCMS_FRONT_END_USER);

		if (isRoleAdministrator) {
			user.setActive(createUserForm.isActive());

			if (!createUserForm.getRoles().isEmpty()) {
				roleKeys = createUserForm.getRoles();
			}
		}

		this.userAPI.save(user, APILocator.systemUser(), false);
		Logger.debug(this,  ()-> "User with userId '" + userId + "' and email '" +
				createUserForm.getEmail() + "' has been created.");

		for (final String roleKey : roleKeys) {

			this.addRole(user, roleKey, true, false);
		}

		return user;
	}


	private void addRole(final User user, final String roleKey, final boolean createRole, final boolean isSystem)
			throws DotDataException {

		Role role = this.roleAPI.loadRoleByKey(roleKey);

		// create the role, in case it does not exist
		if (role == null && createRole) {
			Logger.info(this, "Role with key '" + roleKey + "' was not found. Creating it...");
			role = createNewRole(roleKey, isSystem);
		}

		if (null != role) {
			if (!this.roleAPI.doesUserHaveRole(user, role)) {
				this.roleAPI.addRoleToUser(role, user);
				Logger.debug(this, "Role named '" + role.getName() + "' has been added to user: " + user.getEmailAddress());
			} else {
				Logger.debug(this,
						"User '" + user.getEmailAddress() + "' already has the role '" + role + "'. Skipping assignment...");
			}
		} else {
			Logger.debug(this, "Role named '" + roleKey + "' does NOT exists in dotCMS. Ignoring it...");
		}
	}

	private Role createNewRole(final String roleKey, final boolean isSystem) throws DotDataException {

		Role role = new Role();
		role.setName(roleKey);
		role.setRoleKey(roleKey);
		role.setEditUsers(true);
		role.setEditPermissions(true);
		role.setEditLayouts(true);
		role.setDescription("");
		role.setId(UUIDGenerator.generateUuid());

		final String date = DateUtil.getCurrentDate();

		ActivityLogger.logInfo(ActivityLogger.class, getClass() + " - Adding Role",
				"Date: " + date + "; " + "Role:" + roleKey);
		AdminLogger.log(AdminLogger.class, getClass() + " - Adding Role", "Date: " + date + "; " + "Role:" + roleKey);

		try {
			role = roleAPI.save(role, role.getId());
		} catch (DotDataException | DotStateException e) {
			ActivityLogger.logInfo(ActivityLogger.class, getClass() + " - Error adding Role",
					"Date: " + date + ";  " + "Role:" + roleKey);
			AdminLogger.log(AdminLogger.class, getClass() + " - Error adding Role",
					"Date: " + date + ";  " + "Role:" + roleKey);
			throw e;
		}

		return role;
	}



}
