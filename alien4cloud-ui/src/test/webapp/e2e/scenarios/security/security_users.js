'use strict';

var authentication = require('../../authentication/authentication');
var users = require('../../admin/users');
var navigation = require('../../common/navigation');
var common = require('../../common/common');
var rolesCommon = require('../../common/roles_common.js');
var applications = require('../../applications/applications');
var components = require('../../components/components');

describe('Check that users with an updated role has indeed access to the new role functions/pages', function() {

  beforeEach(function() {
    common.before();
  });

  afterEach(function() {
    // Logout action
    authentication.logout();
  });

  it('user with updated role should be to perform the new role operations', function() {
    console.log('################# user with updated role should be to perform the new role operations');
    // Before adding the role components manager --> it does not work
    authentication.login('admin');
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    //create and check a user
    users.createUser(authentication.users.sauron);
    authentication.reLogin('sauron');
    applications.checkApplicationManager(false);
    components.checkComponentManager(false);

    // Add the role COMPONENTS_MANAGER and APPLICATIONS_MANAGER
    authentication.reLogin('admin');
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    rolesCommon.editUserRole('sauron', rolesCommon.alienRoles.componentsManager);
    rolesCommon.editUserRole('sauron', rolesCommon.alienRoles.applicationsManager);

    authentication.reLogin('sauron');
    applications.checkApplicationManager(true);
    components.checkComponentManager(true);

    // Remove the role COMPONENTS_MANAGER
    authentication.reLogin('admin');
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    rolesCommon.editUserRole('sauron', rolesCommon.alienRoles.componentsManager);
    authentication.reLogin('sauron');
    applications.checkApplicationManager(true);
    components.checkComponentManager(false);

    // Remove the role COMPONENTS_MANAGER
    authentication.reLogin('admin');
    navigation.go('main', 'admin');
    navigation.go('admin', 'users');
    rolesCommon.editUserRole('sauron', rolesCommon.alienRoles.applicationsManager);
    authentication.reLogin('sauron');
    applications.checkApplicationManager(false);
    components.checkComponentManager(false);
  });

});
