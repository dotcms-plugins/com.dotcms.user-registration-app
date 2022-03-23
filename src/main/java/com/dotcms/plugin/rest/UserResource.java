package com.dotcms.plugin.rest;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.glassfish.jersey.server.JSONP;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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

@Path("/v3/users")
public class UserResource {

    private final WebResource webResource = new WebResource();
	private final UserAPI userAPI;
	private final RoleAPI roleAPI;

	public UserResource() {
		this.userAPI = APILocator.getUserAPI();
		this.roleAPI = APILocator.getRoleAPI();
	}

	private final Map<String, List<String>> rolesMap =
			//map("member",     list(Role.DOTCMS_FRONT_END_USER, "cms_member"),
			//	"subscriber", list(Role.DOTCMS_FRONT_END_USER, "cms_subscriber"));

			map("advertiser",     list(Role.DOTCMS_FRONT_END_USER, "advertiser"),
				"seller", list(Role.DOTCMS_FRONT_END_USER, "seller"),
				"leader", list(Role.DOTCMS_FRONT_END_USER, "leader"));

	/**
	 *
	 * @param httpServletRequest
	 * @param createUserForm
	 * @return
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
				// todo: any permission to check
				.init().getUser();

		final User userToUpdated = this.createNewUser(null == modUser? APILocator.systemUser(): modUser, createUserForm);

		// todo: send an action email

		return Response.ok(new ResponseEntityView(map("userID", userToUpdated.getUserId(),
				 "user", userToUpdated.toMap()))).build(); // 200
	} // create.

	protected User createNewUser(final User modUser, final CreateUserForm createUserForm) throws DotDataException, DotSecurityException, ParseException {

		final String nameID       = "userId-" + UUIDUtil.uuid();
		final User user = this.userAPI.createUser(nameID, createUserForm.getEmail());

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

		user.setActive(false); // need validation confirmation email
		this.userAPI.save(user, modUser, false);
		Logger.debug(this,  ()-> "User with NameID '" + nameID + "' and email '" +
				createUserForm.getEmail() + "' has been created.");

		final String type = UtilMethods.isSet(createUserForm.getType())?createUserForm.getType():"subscriber";
		final List<String> roleKeys = this.rolesMap.getOrDefault(type, Collections.emptyList());

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
		role.setEditPermissions(false);
		role.setEditLayouts(false);
		role.setDescription("");
		role.setId(UUIDGenerator.generateUuid());

		// Setting SYSTEM role as a parent
		role.setSystem(isSystem);
		final Role parentRole = roleAPI.loadRoleByKey(Role.SYSTEM);
		role.setParent(parentRole.getId());

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
