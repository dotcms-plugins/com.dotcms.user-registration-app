package com.dotcms.plugin.rest;

import com.dotcms.plugin.app.ActiveType;
import com.dotcms.plugin.app.AppConfig;
import com.dotcms.plugin.app.ConfigService;
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
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.user.ajax.UserAjax;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;

@Path("/v1/myusers")
public class MyUserResource {

    private final WebResource webResource = new WebResource();
	private final UserAPI userAPI;
	private final RoleAPI roleAPI;

	public MyUserResource() {
		this.userAPI = APILocator.getUserAPI();
		this.roleAPI = APILocator.getRoleAPI();
	}

	@GET
	@JSONP
	@Path("/_find/roles/{roleId}/fe-users")
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response findFrontEndUsersByRole(@Context final HttpServletRequest request,
											@Context final HttpServletResponse response,
											@PathParam("roleId")String roleId) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requestAndResponse(request, response)
				.requiredFrontendUser(true)
				.rejectWhenNoUser(true).init().getUser();

		Logger.debug(this, ()-> "Finding FE users by role: " + roleId);
		final List<User> users = new ArrayList<>();
		final Role role = this.roleAPI.loadRoleById(roleId);
		if (null == role) {

			throw new DoesNotExistException("Role not found: " + roleId);
		}

		return Response.ok(new ResponseEntityView(
				this.roleAPI.findUsersForRole(role).stream().filter(User::isFrontendUser)
						.map(user -> new SimpleUserView(user.getFirstName(), user.getLastName(), user.getEmailAddress()))
						.collect(Collectors.toList())
		)).build(); // 200
	} // getCurrentUserRoles.

	@GET
	@JSONP
	@Path("/roles")
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final Response getCurrentUserRoles(@Context final HttpServletRequest request,
								 @Context final HttpServletResponse response) throws Exception {

		final User modUser = new WebResource.InitBuilder(webResource)
				.requestAndResponse(request, response)
				.rejectWhenNoUser(true).init().getUser();

		Logger.debug(this, ()-> "Getting current User roles for: " + modUser.getUserId());
		final List<Map<String, Object>> roleMaps = new ArrayList<>();
		final Role userRole    = this.roleAPI.loadRoleByKey(RoleAPI.USERS_ROOT_ROLE_KEY);
		final List<Role> roles = this.roleAPI.loadRolesForUser(modUser.getUserId(), false);
		roles.stream().filter(role -> !role.getDBFQN().contains(userRole.getId())).forEach(role -> roleMaps.add(role.toMap()));

		return Response.ok(new ResponseEntityView(roleMaps)).build(); // 200
	} // getCurrentUserRoles.


	@GET
	@JSONP
	@NoCache
	@Produces({MediaType.APPLICATION_JSON, "application/javascript"})
	public final String hello(@Context final HttpServletRequest httpServletRequest,
							  @Context final HttpServletResponse httpServletResponse) throws Exception {



		return "Hello there"; // 200
	} // create.

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
		final Optional<AppConfig> config = ConfigService.INSTANCE.config(WebAPILocator.getHostWebAPI().getHost(httpServletRequest));
		final User userToUpdated = this.createNewUser(
				null != modUser? APILocator.getUserAPI().getAnonymousUser(): modUser,
				isRoleAdministrator, createUserForm, config);

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
								 final CreateUserForm createUserForm,
								 final Optional<AppConfig> optAppConfig)
			throws DotDataException, DotSecurityException, ParseException {

		final String userId = UtilMethods.isSet(createUserForm.getUserId())?
				createUserForm.getUserId(): "userId-" + UUIDUtil.uuid();
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

		checkActiveUser(user, isRoleAdministrator, createUserForm, optAppConfig);
		final List<String> roleKeys = processRole(user, createUserForm, optAppConfig, list(Role.DOTCMS_FRONT_END_USER));

		this.userAPI.save(user, APILocator.systemUser(), false);
		Logger.debug(this,  ()-> "User with userId '" + userId + "' and email '" +
				createUserForm.getEmail() + "' has been created.");

		for (final String roleKey : roleKeys) {

			this.addRole(user, roleKey, false	, false);
		}

		return user;
	}

	private List<String> processRole(final User user, final CreateUserForm createUserForm,
									 final Optional<AppConfig> optAppConfig, final List<String> defaultRoles) {

		if (optAppConfig.isPresent() &&
				UtilMethods.isSet(optAppConfig.get().getRolesWhiteList()) &&
				UtilMethods.isSet(createUserForm.getRoles())) {

			return createUserForm.getRoles().stream()
					.filter(rolekey -> optAppConfig.get().getRolesWhiteList().contains(rolekey))
					.collect(Collectors.toList());
		}

		return UtilMethods.isSet(createUserForm.getRoles())? createUserForm.getRoles(): defaultRoles;
	}

	private void checkActiveUser(final User user, final boolean isRoleAdministrator,
								 final CreateUserForm createUserForm, final Optional<AppConfig> optAppConfig) {

		user.setActive(false);
		optAppConfig.ifPresent(theConfig -> {

			if (theConfig.getActiveType() != ActiveType.INACTIVE) {

				if (theConfig.getActiveType() == ActiveType.ACTIVE) {

					user.setActive(createUserForm.isActive());
				}

				if (theConfig.getActiveType() == ActiveType.ONLY_ADMIN && isRoleAdministrator) {

					user.setActive(createUserForm.isActive());
				}
			}
		});
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
