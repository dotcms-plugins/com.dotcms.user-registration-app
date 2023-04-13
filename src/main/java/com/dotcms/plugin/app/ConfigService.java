package com.dotcms.plugin.app;

import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ConfigService {

    public static final ConfigService INSTANCE = new ConfigService();

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEMM_HOST for a
     * valid configuration. This lookup is low overhead and cached by dotCMS.
     *
     * @param host
     * @return
     */
    public Optional<AppConfig> config(final Host host) {

        final Optional<AppSecrets> appSecrets = Try.of(
                        () -> APILocator.getAppsAPI().getSecrets(AppKeys.APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());

        if (!appSecrets.isPresent()) {

            Logger.debug(this.getClass().getName(), ()-> "App secrets is empty for host: " + host.getHostname());
            return Optional.empty();
        }

        final Map<String, Secret> secrets = appSecrets.get().getSecrets();
        final String activeKey   = Try.of(()->secrets
                .get(AppKeys.ACTIVE_KEY.key).getString()).getOrElse(StringPool.BLANK);
        final String rolesWhiteListKey = Try.of(()->secrets
                .get(AppKeys.ROLE_WHITE_LIST_KEY.key).getString()).getOrElse(StringPool.BLANK);

        Logger.debug(this.getClass().getName(), ()-> "activeKey: " + activeKey);
        Logger.debug(this.getClass().getName(), ()-> "rolesWhiteListKey: " + rolesWhiteListKey);

        final ActiveType  activeType     = ActiveType.valueOfType(activeKey);
        final Set<String> rolesWhiteList = processRoles(rolesWhiteListKey);

        final AppConfig config = new AppConfig(
                rolesWhiteList, activeType);

        return Optional.ofNullable(config);
    }


    private Set<String> processRoles (final String paramKey) {

        final Set<String> roleSet = new HashSet<>();

        if (StringUtils.isSet(paramKey)) {

            Logger.debug(this.getClass().getName(), ()-> "paramKey: " + paramKey);
            final String[] rolesEntries = org.apache.commons.lang3.
                    StringUtils.split(paramKey, StringPool.COMMA);

            if (null != rolesEntries) {

                roleSet.addAll(Arrays.asList(rolesEntries));
            }
        }

        return roleSet;
    }
}
