'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var navigation = require('../common/navigation');
var rolesCommon = require('../common/roles_common.js');
var applications = require('../applications/applications');
var users = require('../admin/users');

describe('Security management on applications for application manager', function() {

  var applicationName = 'Alien';

  beforeEach(function() {
    common.before();

    // create user
    authentication.login('admin');

    applications.createApplication(applicationName, 'Great great app...');

    // create group
    users.navigationGroups();
    users.createGroup(users.groups.mordor);

    users.navigationUsers();
    users.createUser(authentication.users.sauron);
    rolesCommon.addUserToGroup(authentication.users.sauron.username, users.groups.mordor.name);
  });

  afterEach(function() {
    common.after();
  });

  var checkAccess = function(menu) {
    navigation.isNavigable('applications', menu);
    navigation.go('applications', menu);
    common.expectNoErrors();
  };

  var checkApplicationManagerAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    checkAccess('topology');
    checkAccess('plan');
    checkAccess('deployment');
    checkAccess('runtime');
    checkAccess('users');
    checkAccess('info');
  };

  var checkApplicationDeploymentManagerAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    // It must has access to every tab of the application
    navigation.isNotNavigable('applications', 'topology');
    navigation.isNotNavigable('applications', 'plan');
    checkAccess('deployment');
    checkAccess('runtime');
    navigation.isNotNavigable('applications', 'users');
    checkAccess('info');
  };

  var checkApplicationDevOpsAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    // It must has access to every tab of the application
    checkAccess('topology');
    checkAccess('plan');
    navigation.isNotNavigable('applications', 'deployment');
    navigation.isNotNavigable('applications', 'runtime');
    navigation.isNotNavigable('applications', 'users');
    checkAccess('info');
  };

  var checkApplicationUserAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    // It must has access to every tab of the application
    navigation.isNotNavigable('applications', 'topology');
    navigation.isNotNavigable('applications', 'plan');
    navigation.isNotNavigable('applications', 'deployment');
    navigation.isNotNavigable('applications', 'runtime');
    navigation.isNotNavigable('applications', 'users');
    checkAccess('info');
  };

  it('should be able to navigate as an application manager in the application if user has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    rolesCommon.editUserRole(authentication.users.sauron.username, rolesCommon.appRoles.appManager);
    checkApplicationManagerAccess();
  });

  it('should be able to navigate as an application manager in the application if user is in a group which has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRole(users.groups.mordor.name, rolesCommon.appRoles.appManager);
    checkApplicationManagerAccess();
  });

  it('should be able to navigate as an application deployment manager in the application if user has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    rolesCommon.editUserRole(authentication.users.sauron.username, rolesCommon.appRoles.deploymentManager);
    checkApplicationDeploymentManagerAccess();
  });

  it('should be able to navigate as an application deployment manager in the application if user is in a group which has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRole(users.groups.mordor.name, rolesCommon.appRoles.deploymentManager);
    checkApplicationDeploymentManagerAccess();
  });

  it('should be able to navigate as an application dev ops in the application if user has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    rolesCommon.editUserRole(authentication.users.sauron.username, rolesCommon.appRoles.appDevops);
    checkApplicationDevOpsAccess();
  });

  it('should be able to navigate as an application dev ops in the application if user is in a group which has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRole(users.groups.mordor.name, rolesCommon.appRoles.appDevops);
    checkApplicationDevOpsAccess();
  });

  it('should be able to navigate as an application user in the application if user has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    rolesCommon.editUserRole(authentication.users.sauron.username, rolesCommon.appRoles.appUser);
    checkApplicationUserAccess();
  });

  it('should be able to navigate as an application user in the application if user is in a group which has this right', function() {
    applications.goToApplicationDetailPage(applicationName);
    navigation.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRole(users.groups.mordor.name, rolesCommon.appRoles.appUser);
    checkApplicationUserAccess();
  });
});