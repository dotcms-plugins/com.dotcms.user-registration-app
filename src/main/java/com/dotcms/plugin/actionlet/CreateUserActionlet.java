package com.dotcms.plugin.actionlet;

import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.workflows.actionlet.EmailActionlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UUIDUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.list;

/**
 * Creates a new user based on the content information
 * @author jsanca
 */
public class CreateUserActionlet extends WorkFlowActionlet {

    private static final long serialVersionUID = 1L;

    @Override
    public List<WorkflowActionletParameter> getParameters() {

        final List<WorkflowActionletParameter> params = new ArrayList<>();

        params.add(new WorkflowActionletParameter("firstNameMapping", "First Name Mapping", "firstName", true));
        params.add(new WorkflowActionletParameter("middleNameMapping", "Middle Name Mapping", "middleName", true));
        params.add(new WorkflowActionletParameter("lastNameMapping", "Last Name Mapping", "lastName", true));
        params.add(new WorkflowActionletParameter("nickNameMapping", "Nick Name Mapping", "nickName", true));
        params.add(new WorkflowActionletParameter("emailMapping", "Email Mapping", "email", true));
        params.add(new WorkflowActionletParameter("maleMapping", "Male Mapping", "male", true));
        params.add(new WorkflowActionletParameter("birthdayMapping", "Birthday Mapping", "birthday", true));
        params.add(new WorkflowActionletParameter("languageIdMapping", "LanguageId Mapping", "languageId", true));
        params.add(new WorkflowActionletParameter("timeZoneIdMapping", "TimeZoneId Mapping", "timeZoneId", true));
        params.add(new WorkflowActionletParameter("passwordMapping", "Password Mapping", "password", true));
        params.add(new WorkflowActionletParameter("additionalInfoMapping", "Additional Information Mapping", "additionalInfo", true));
        params.add(new WorkflowActionletParameter("active", "Is Active", "false", true));
        params.add(new WorkflowActionletParameter("roles", "Roles (by commas)", "", false));

        return params;
    }

    @Override
    public String getName() {
        return "Create User";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will create an user based on the current contentlet, can customize the mappings for the user properties and set roles (optional) and activate or not the user";
    }

    @Override
    public void executeAction(final WorkflowProcessor processor, final Map<String, WorkflowActionClassParameter> params)
            throws WorkflowActionFailureException {

        final Contentlet currentContenlet = processor.getContentlet();

        try {

            final String active     = params.get("active").getValue();
            final String roles      = params.get("roles").getValue();
            final String userId     = "userId-" + UUIDUtil.uuid();
            final String firstName  = currentContenlet.getStringProperty(params.get("firstNameMapping").getValue());
            final String middleName = currentContenlet.getStringProperty(params.get("middleNameMapping").getValue());
            final String lastName   = currentContenlet.getStringProperty(params.get("lastNameMapping").getValue());
            final String nickName   = currentContenlet.getStringProperty(params.get("nickNameMapping").getValue());
            final String email      = currentContenlet.getStringProperty(params.get("emailMapping").getValue());
            final String male       = currentContenlet.getStringProperty(params.get("maleMapping").getValue());
            final String birthday   = currentContenlet.getStringProperty(params.get("birthdayMapping").getValue());
            final String languageId = currentContenlet.getStringProperty(params.get("languageIdMapping").getValue());
            final String timeZoneId = currentContenlet.getStringProperty(params.get("timeZoneIdMapping").getValue());
            final String password   = currentContenlet.getStringProperty(params.get("passwordMapping").getValue());
            final String additionalInfo = currentContenlet.getStringProperty(params.get("additionalInfoMapping").getValue());

            createUser(active, roles, userId, firstName, middleName, lastName, nickName, email, male,
                    birthday, languageId, timeZoneId, password, additionalInfo);
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    } // executeAction.

    private void createUser(final String active, final String roles,
                                   final String userId, final String firstName,
                                   final String middleName, final String lastName,
                                   final String nickName, final String email,
                                   final String male, final String birthday,
                                   final String languageId, final String timeZoneId,
                                   final String password, final String additionalInfo) throws DotDataException, JsonProcessingException, ParseException, DotSecurityException {

        final User user = APILocator.getUserAPI().createUser(userId, email);

        Optional.ofNullable(firstName).ifPresent(user::setFirstName);
        Optional.ofNullable(lastName).ifPresent(user::setLastName);
        Optional.ofNullable(middleName).ifPresent(user::setMiddleName);
        Optional.ofNullable(nickName).ifPresent(user::setNickName);
        Optional.ofNullable(timeZoneId).ifPresent(user::setTimeZoneId);
        Optional.ofNullable(password).ifPresent(user::setPassword);

        if (UtilMethods.isSet(additionalInfo)) {

            final Map userInfoMap =
                    DotObjectMapperProvider.getInstance().getDefaultObjectMapper().readValue(additionalInfo, Map.class);

            user.setAdditionalInfo(userInfoMap);
        }

        if (UtilMethods.isSet(birthday)) {
            user.setBirthday(DateUtil.parseISO(birthday));
        }

        if (UtilMethods.isSet(languageId)) {

            final long languageIdLong = ConversionUtils.toLong(languageId, 0l);
            user.setLanguageId(String.valueOf(languageIdLong <= 0 ?
                    APILocator.getLanguageAPI().getDefaultLanguage().getId() : languageIdLong));
        }

        user.setMale(ConversionUtils.toBoolean(male, false));
        user.setActive(ConversionUtils.toBoolean(active, false));
        user.setCreateDate(new Date());

        final List<String> roleKeys = UtilMethods.isSet(roles)?list(roles.split(StringPool.COMMA)):list(Role.DOTCMS_FRONT_END_USER);

        APILocator.getUserAPI().save(user, APILocator.systemUser(), false);
        Logger.info(this,  ()-> "User with userId '" + userId + "' and email '" + email + "' has been created.");

        for (final String roleKey : roleKeys) {

            this.addRole(user, roleKey, false	, false);
        }
    }

    // todo: this should be moved to a common method
    private void addRole(final User user, final String roleKey, final boolean createRole, final boolean isSystem)
            throws DotDataException {

        Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);

        // create the role, in case it does not exist
        if (role == null && createRole) {
            Logger.info(this, "Role with key '" + roleKey + "' was not found. Creating it...");
            role = createNewRole(roleKey, isSystem);
        }

        if (null != role) {
            if (!APILocator.getRoleAPI().doesUserHaveRole(user, role)) {
                APILocator.getRoleAPI().addRoleToUser(role, user);
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
            role = APILocator.getRoleAPI().save(role, role.getId());
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
