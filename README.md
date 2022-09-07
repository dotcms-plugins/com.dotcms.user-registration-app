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

Once installed, you can access this resource by:

```
POST /api/v1/users/

{
"firstName":"Test User",  
"lastName":"Test User",
"email":"test.user@dotcms.com",
"password":[ "P","a","s","s","w","o","r","d","1","2","3","." ],
"roles": ["rolekey1","rolekey2",...],
"active": true|false
}
```

Required Properties:

- firstName = string
- lastName = string
- email = string
- password = char[]

Other properties:
- Active = boolean, default false
- Roles = list of rolekey
- userId = string
	 
Scenarios:

	 1. No Auth or User doing the request do not have access to Users and Roles Portlets
	 - Always will be inactive
	 - Only the	Role DOTCMS_FRONT_END_USER will be added
	 2. Auth, User is Admin or have access to Users and Roles Portlets
	 - Can be active if JSON includes ("active": true)
	 - The list of RoleKey will be use to assign the roles, if the roleKey doesn't exist will be
	 created under the ROOT ROLE.


## Authentication

This API allows anon users to call it, but users created that way will be inactive and will only have DOTCMS_FRONT_END_USER role added.




