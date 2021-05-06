package com.dotcms.plugin.rest;

import com.dotcms.repackage.javax.validation.constraints.NotNull;
import com.dotcms.repackage.org.hibernate.validator.constraints.NotBlank;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.api.v1.user.UpdateUserForm;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Date;
import java.util.Map;

/**
 * Encapsulates the information to create an User
 */
@JsonDeserialize(builder = CreateUserForm.Builder.class)
public final class CreateUserForm extends Validated  {


    @NotNull
    @NotBlank
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String nickName;
    @NotNull
    @NotBlank
    private final String email;
    private final boolean male;
    private final String  birthday;
    private final long    languageId;
    private final String  timeZoneId;
    private final char[] password;
    @NotNull
    @NotBlank
    private String    type;
    private final Map<String, String> additionalInfo;

    private CreateUserForm(CreateUserForm.Builder builder) {

        this.firstName = builder.firstName;
        this.middleName = builder.middleName;
        this.lastName = builder.lastName;
        this.nickName = builder.nickName;
        this.email = builder.email;
        this.male = builder.male;
        this.birthday = builder.birthday;
        this.languageId = builder.languageId;
        this.timeZoneId = builder.timeZoneId;
        this.password = builder.password;
        this.type     = builder.type;
        this.additionalInfo = builder.additionalInfo;

        checkValid();
        if (!UtilMethods.isSet(this.password)) {
            throw new IllegalArgumentException("Password can not be null");
        }
    }

    public String getType() {
        return type;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMale() {
        return male;
    }

    public String getBirthday() {
        return birthday;
    }

    public long getLanguageId() {
        return languageId;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public char[] getPassword() {
        return password;
    }

    public Map<String, String> getAdditionalInfo() {
        return additionalInfo;
    }

    public static final class Builder {
        @JsonProperty private String firstName;
        @JsonProperty private String middleName;
        @JsonProperty private String lastName;
        @JsonProperty private String nickName;
        @JsonProperty private String email;
        @JsonProperty private boolean male;
        @JsonProperty private String  birthday;
        @JsonProperty private long    languageId = -1l;
        @JsonProperty private String    timeZoneId;
        @JsonProperty private char[]    password;
        @JsonProperty private String    type;
        @JsonProperty private Map<String, String>    additionalInfo;

        public Builder() {
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder middleName(String middleName) {
            this.middleName = middleName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder nickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder male(boolean male) {
            this.male = male;
            return this;
        }

        public Builder birthday(String birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder languageId(long languageId) {
            this.languageId = languageId;
            return this;
        }

        public Builder timeZoneId(String timeZoneId) {
            this.timeZoneId = timeZoneId;
            return this;
        }

        public Builder password(char[] password) {
            this.password = password;
            return this;
        }

        public Builder additionalInfo(Map<String, String>    additionalInfo) {
            this.additionalInfo = additionalInfo;
            return this;
        }

        public CreateUserForm build() {
            return new CreateUserForm(this);
        }
    }
}
 
