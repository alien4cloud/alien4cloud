'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var rolesCommon = require('../../common/roles_common.js');
var users = require('../../admin/users');
var applications = require('../../applications/applications');
var components = require('../../components/components');

describe('Security management for group in action', function() {

  beforeEach(function() {
    common.before();
    authentication.login('admin');

    users.navigationUsers();
    users.createUser(authentication.users.sauron);
    users.createUser(authentication.users.bilbo);
    users.createGroup(users.groups.mordor);

    // create group
    users.navigationGroups();
    users.createGroup(users.groups.mordor);
    users.createGroup(users.groups.shire);
  });

  afterEach(function() {
    common.after();
  });

  it('should be able to add/remove user from a group and user roles are updated', function() {
    console.log('################# should be able to add/remove user from a group and user roles are updated');
    // User is not in the group
    authentication.reLogin('admin');
    users.navigationGroups();
    rolesCommon.editGroupRole('mordor', rolesCommon.alienRoles.applicationsManager);
    authentication.reLogin('sauron');
    applications.checkApplicationManager(false);

    // User is in the group, he has the role now
    authentication.reLogin('admin');
    users.navigationUsers();
    rolesCommon.addUserToGroup('sauron', 'mordor');
    authentication.reLogin('sauron');
    applications.checkApplicationManager(true);

    // User is not in the group, he does not have the role anymore
    authentication.reLogin('admin');
    users.navigationUsers();
    rolesCommon.removeUserFromGroup('sauron', 'mordor');
    authentication.reLogin('sauron');
    applications.checkApplicationManager(false);
  });

  it('should be able to add/remove role to a group of user and user roles are updated', function() {
    console.log('################# should be able to add/remove role to a group of user and user roles are updated');
    // User is not in the group
    authentication.reLogin('admin');
    users.navigationUsers();
    rolesCommon.addUserToGroup('bilbo', 'shire');
    authentication.reLogin('bilbo');
    components.checkComponentManager(false);

    // User is in the group, he has the role now
    authentication.reLogin('admin');
    users.navigationGroups();
    rolesCommon.editGroupRole('shire', rolesCommon.alienRoles.componentsManager);
    authentication.reLogin('bilbo');
    components.checkComponentManager(true);

    // User is not in the group, he does not have the role anymore
    authentication.reLogin('admin');
    users.navigationGroups();
    rolesCommon.editGroupRole('shire', rolesCommon.alienRoles.componentsManager);
    authentication.reLogin('bilbo');
    components.checkComponentManager(false);
  });

  it('should be able to delete a group of user and user roles are updated ', function() {
    console.log('################# should be able to delete a group of user and user roles are updated');
    // User is not in the group
    authentication.reLogin('admin');
    users.navigationUsers();
    rolesCommon.addUserToGroup('bilbo', 'shire');
    users.navigationGroups();
    rolesCommon.editGroupRole('shire', rolesCommon.alienRoles.componentsManager);
    authentication.reLogin('bilbo');
    components.checkComponentManager(true);

    // Remove the group, the guy do not have role anymore
    authentication.reLogin('admin');
    users.navigationGroups();
    users.deleteGroup('shire');
    authentication.reLogin('bilbo');
    components.checkComponentManager(false);
  });

});
