#--------------------------------------------------------------------
# To debug integration test
#--------------------------------------------------------------------

mvn -Dmaven.failsafe.debug clean verify

On eclipse run a remote debug session on the port 5005


#--------------------------------------------------------------------
# Mock plugin files update when the main mock plugin changes
#--------------------------------------------------------------------

Main idea : the plugin 1.0 is uploaded is uploaded and configured by default, and then we try to upload new plugin version 
and test some cases (recover the plugin configuration from old plugin version to the new version if possible)

- good-plugin-1.1.jar : corresponds to the "next" jar version of the current plugin version in plugin.yml
  * If the current version is 1.0 in plugin.yml then this file should be the exact same build with 1.1 version instead of 1.0
- good-plugin-different-confType-1.1.jar : corresponds to the "next" jar version of the current plugin version in plugin.yml
  * The difference with good-plugin-1.1.jar is that the ProviderConfig.java is different (differents config fields for example)
- invalid-plugin-without-yml.jar : well named, this is a bad plugin jar, with missing plugin.yml file

WARNING : tests with those 1.1 version files imply that the 1.0 version is loaded first (usually in Background IT Tests)

Launch mnv tests with cucumber tags :

mvn verify -DdoTest -pl alien4cloud-rest-it -Dcucumber.options="--tags @loic"
