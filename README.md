# README

This is an example of how to create and load Jersey Based REST resource, that creates an user in dotCMS via OSGi 

## How to build this example

To install all you need to do is build the JAR. to do this run
`./gradlew jar`

This will build two jars in the `build/libs` directory: a bundle fragment (in order to expose needed 3rd party libraries from dotCMS) and the plugin jar 

* **To install this bundle:**

    Copy the bundle jar files inside the Felix OSGI container (*dotCMS/felix/load*).
        
    OR
        
    Upload the bundle jars files using the dotCMS UI (*CMS Admin->Dynamic Plugins->Upload Plugin*).

* **To uninstall this bundle:**
    
    Remove the bundle jars files from the Felix OSGI container (*dotCMS/felix/load*).

    OR

    Undeploy the bundle jars using the dotCMS UI (*CMS Admin->Dynamic Plugins->Undeploy*).

---
## How to use

Before building the plugin add custom mappings in the UserResource:

```
/**
	 * Map of roles
	 * The key is the type we sent in the JSON Body
	 * The value should be the list of roles that we want the user get assigned, the role is search by key NOT by name.
	 * In case a roleKey is sent and it doesn't exists, the role will be created under the Root Role.
	 *
	 * Note: A user needs the Role.DOTCMS_BACK_END_USER to be able to log in into the dotCMS.
	 *
	 */
private final Map<String, List<String>> rolesMap =
			map(
					//DO NOT REMOVE THESE 3 MAPPINGS
					"frontend",     list(Role.DOTCMS_FRONT_END_USER),
				"backend", list(Role.DOTCMS_BACK_END_USER),
				"admin", list(Role.CMS_ADMINISTRATOR_ROLE,Role.DOTCMS_BACK_END_USER)
					//ADD NEW MAPPINGS BELOW
					//e.g "publisher", list(Role.DOTCMS_BACK_END_USER,"publisher")
			);

```

Once installed, you can access this resource by:

```
POST /api/v1/users/

{
"firstName":"Test User",  
"lastName":"Test User",
"email":"test.user@dotcms.com",
"password":[ "P","a","s","s","w","o","r","d","1","2","3","." ],
"type":"backend"
}
```



## Authentication

This API supports the same REST auth infrastructure as other 
rest apis in dotcms. There are 4 ways to authenticate.

* user/xxx/password/yyy in the URI
* basic http/https authentication (base64 encoded)
* DOTAUTH header similar to basic auth and base64 encoded, e.g. setHeader("DOTAUTH", base64.encode("admin@dotcms.com:admin"))
* Session based (form based login) for frontend or backend logged in user
* Using a JWT bearer on the Authentication Header
* User needs access to the USERS and ROLES portlet.




