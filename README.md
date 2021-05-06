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

## How to create a bundle plugin for a rest resource

In order to create this OSGI plugin, you must create a `META-INF/MANIFEST` to be inserted into OSGI jar.
This file is being created for you by Gradle. If you need you can alter our config for this but in general our out of the box config should work.
The Gradle plugin uses BND to generate the Manifest. The main reason you need to alter the config is when you need to exclude a package you are including on your Bundle-ClassPath

If you are building the MANIFEST on your own or desire more info on it below is a description of what is required in this MANIFEST you must specify (see template plugin):

```
    Bundle-Name: The name of your bundle
    Bundle-SymbolicName: A short an unique name for the bundle
    Bundle-Activator: Package and name of your Activator class (example: com.dotmarketing.osgi.override.Activator)
    Export-Package: Declares the packages that are visible outside the plugin. Any package not declared here has visibility only within the bundle.
    Import-Package: This is a comma separated list of the names of packages to import. In this list there must be the packages that you are using inside your osgi bundle plugin and are exported and exposed by the dotCMS runtime.
```

## Beware (!)

In order to work inside the Apache Felix OSGI runtime, the import and export directive must be bidirectional, there are two ways to accomplish this:

* **Exported Packages**

    The dotCMS must declare the set of packages that will be available to the OSGI plugins by changing the file: *dotCMS/WEB-INF/felix/osgi-extra.conf*.
This is possible also using the dotCMS UI (*CMS Admin->Dynamic Plugins->Exported Packages*).

    Only after that exported packages are defined in this list, a plugin can Import the packages to use them inside the OSGI blundle.
    
* **Fragment**

    A Bundle fragment, is a bundle whose contents are made available to another bundles exporting 3rd party libraries from dotCMS.
One notable difference is that fragments do not participate in the lifecycle of the bundle, and therefore cannot have an Bundle-Activator.
As it not contain a Bundle-Activator a fragment cannot be started so after deploy it will have its state as Resolved and NOT as Active as a normal bundle plugin.

---
## How to test

Once installed, you can access this resource by (this assumes you are on localhost)

`curl --location --request POST 'http://localhost:8080/api/v3/users/' \
--header 'Authorization: Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg==' \
--header 'Content-Type: application/json' \
--header 'Cookie: access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI0NjE3NmUxMi02YzJkLTQwNmMtYjcyNC00ZGZhYzQ1NjI1MTciLCJ4bW9kIjoxNjIwMzIzMjQxODI4LCJzdWIiOiJkb3RjbXMub3JnLjEiLCJpYXQiOjE2MjAzMjMyNDEsImlzcyI6IjAyMTY5MDZkYjAiLCJleHAiOjE2MjA0MDk2NDF9.4YYCIqSRVD3tQ9sPPsp8-kyz8nEx3JT6AlLRx_yuNpQ' \
--data-raw '{
"firstName":"Test Fe User",
"middleName":"T",
"lastName":"lastName",
"nickName":"test.fe.user",
"email":"test.fe.user@dotcms.com",
"male":true,
"birthday":"1990-11-20",
"languageId":1,
"timeZoneId":"Etc/GMT+10",
"password":[
"P","a","s","s","w","o","r","d","1","2","3","."
],
"type":"member",
"additionalInfo":{
"address":"CR, SanJose, San Jose, San Pedro",
"postalCode":"50101",
"country":"CR"
}
}   `

## Authentication

This API supports the same REST auth infrastructure as other 
rest apis in dotcms. There are 4 ways to authenticate.

* user/xxx/password/yyy in the URI
* basic http/https authentication (base64 encoded)
* DOTAUTH header similar to basic auth and base64 encoded, e.g. setHeader("DOTAUTH", base64.encode("admin@dotcms.com:admin"))
* Session based (form based login) for frontend or backend logged in user
* Using a JWT bearer on the Authentication Header


## Querying

As part of this plugin you may see how we introduced the additional info field
This field adds the ability to insert into the user any json structure in addition to the 
normal user information, this give us the ability to extend the user entity by using a mutable 
and schemeless json

In terms of SQL (only on postgresql) you can do things such as

select additional_info -> 'address'  from User_  -- selects only the adress field into the json


select additional_info from User_ where  additional_info ->> 'country' = 'US';  --  filter by country field  




