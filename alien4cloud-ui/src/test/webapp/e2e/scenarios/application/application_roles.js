/* global describe, it, by, element, expect */
'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var users = require('../../admin/users');
var rolesCommon = require('../../common/roles_common');
var applications = require('../../applications/applications');

var applicationsData = require(__dirname + '/_data/application_roles/applications.json');
var applicationEnvironmentsData = require(__dirname + '/_data/application_roles/applicationenvironments.json');
var applicationVersionsData = require(__dirname + '/_data/application_roles/applicationversions.json');
var topologiesData = require(__dirname + '/_data/application_roles/topologies.json');

describe('Security management on applications', function() {
  var defaultApplicationName = 'AlienUITest';
  var otherApplicationName = 'ApplicationRolesTestOtherApp';

  var checkAccess = function(menu) {
    common.isNavigable('applications', menu);
    common.go('applications', menu);
    toaster.expectNoErrors();
  };

  var checkDisplayedButDisabled = function(menu) {
    common.isPresentButDisabled('applications', menu);
  };

  var checkApplicationManagerAccess = function(appName) {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(appName);
    checkAccess('topology');
    checkAccess('deployment');
    checkDisplayedButDisabled('runtime');
    checkAccess('users');
    checkAccess('info');
    authentication.reLogin('applicationManager');
  };

  var checkApplicationDeploymentManagerAccess = function(appName) {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(appName);
    checkAccess('info');
    checkAccess('deployment');
    checkDisplayedButDisabled('runtime');
    common.isNotNavigable('applications', 'topology');
    common.isNotNavigable('applications', 'users');
    authentication.reLogin('applicationManager');
  };

  var checkApplicationDevOpsAccess = function(appName) {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(appName);
    checkAccess('topology');
    common.isNotNavigable('applications', 'deployment');
    common.isNotNavigable('applications', 'runtime');
    common.isNotNavigable('applications', 'users');
    checkAccess('info');
    authentication.reLogin('applicationManager');
  };

  var checkApplicationUserAccess = function(appName) {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(appName);
    common.isNotNavigable('applications', 'topology');
    common.isNotNavigable('applications', 'deployment');
    common.isNotNavigable('applications', 'runtime');
    common.isNotNavigable('applications', 'users');
    checkAccess('info');
    authentication.reLogin('applicationManager');
  };

  var checkNoAccessToApp = function(appName) {
    authentication.reLogin(authentication.users.sauron.username);
    applications.go();
    applications.searchApplication(appName);
    expect(element(by.id('app_' + appName)).isPresent()).toBe(false);
    authentication.reLogin('applicationManager');
  };

  var toggleUserRole = function(role, hasRole, appName) {
    applications.go();
    applications.goToApplicationDetailPage(appName);
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    rolesCommon.editUserRole(authentication.users.sauron.username, role);
    common.go('applications', 'info');
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    if (hasRole) {
      rolesCommon.assertUserHasRoles(authentication.users.sauron.username, role);
    } else {
      rolesCommon.assertUserDoesNotHaveRoles(authentication.users.sauron.username, role);
    }
  };

  var toggleGroupRole = function(role, hasRole, appName) {
    applications.goToApplicationDetailPage(appName);
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRole(users.groups.mordor.name, role);
    common.go('applications', 'info');
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    if (hasRole) {
      rolesCommon.assertGroupHasRoles(users.groups.mordor.name, role);
    } else {
      rolesCommon.assertGroupDoesNotHaveRoles(users.groups.mordor.name, role);
    }
  };

  var toggleUserRoleForEnv = function(role, hasRole, appName) {
    applications.goToApplicationDetailPage(appName);
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    rolesCommon.editUserRoleForEnv(authentication.users.sauron.username, role);
    common.go('applications', 'info');
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    if (hasRole) {
      rolesCommon.assertUserHasRolesForEnv(authentication.users.sauron.username, role);
    } else {
      rolesCommon.assertUserDoesNotHaveRolesForEnv(authentication.users.sauron.username, role);
    }
    common.selectBSDropdown(by.id('users_environment_switcher'), 'SecondEnvironment');
    rolesCommon.assertUserDoesNotHaveRolesForEnv(authentication.users.sauron.username, role);
  };

  var toggleGroupRoleForEnv = function(role, hasRole, appName) {
    applications.goToApplicationDetailPage(appName);
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRoleForEnv(users.groups.mordor.name, role);
    common.go('applications', 'info');
    common.go('applications', 'users');
    common.selectBSDropdown(by.id('users_environment_switcher'), 'Environment');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    if (hasRole) {
      rolesCommon.assertGroupHasRolesForEnv(users.groups.mordor.name, role);
    } else {
      rolesCommon.assertGroupDoesNotHaveRolesForEnv(users.groups.mordor.name, role);
    }
    common.selectBSDropdown(by.id('users_environment_switcher'), 'SecondEnvironment');
    rolesCommon.assertGroupDoesNotHaveRolesForEnv(users.groups.mordor.name, role);
  };

  it('beforeAll', function() {
    setup.setup();
    setup.index('application', 'application', applicationsData);
    setup.index('applicationenvironment', 'applicationenvironment', applicationEnvironmentsData);
    setup.index('applicationversion', 'applicationversion', applicationVersionsData);
    setup.index('topology', 'topology', topologiesData);
    common.home();
    authentication.login('admin');
  });

  it('should be able to add role to others user on the application if user is admin, and application_manager must be per application basis', function() {
    toggleUserRole(rolesCommon.appRoles.appManager, true, defaultApplicationName);
    checkApplicationManagerAccess(defaultApplicationName);
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, true, otherApplicationName);
    checkApplicationUserAccess(otherApplicationName);
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, false, otherApplicationName);
    toggleUserRole(rolesCommon.appRoles.appManager, false, defaultApplicationName);
  });

  it('should be able to navigate as an application manager in the application if user has this right', function() {
    toggleUserRole(rolesCommon.appRoles.appManager, true, defaultApplicationName);
    checkApplicationManagerAccess(defaultApplicationName);
    toggleUserRole(rolesCommon.appRoles.appManager, false, defaultApplicationName);
    checkNoAccessToApp(defaultApplicationName);
  });

  it('should be able to navigate as an application manager in the application if user is in a group which has this right', function() {
    toggleGroupRole(rolesCommon.appRoles.appManager, true, defaultApplicationName);
    checkApplicationManagerAccess(defaultApplicationName);
    toggleGroupRole(rolesCommon.appRoles.appManager, false, defaultApplicationName);
  });

  it('should be able to navigate as an application deployment manager in the application if user has this right', function() {
    toggleUserRoleForEnv(rolesCommon.envRoles.deploymentManager, true, defaultApplicationName);
    checkApplicationDeploymentManagerAccess(defaultApplicationName);
    toggleUserRoleForEnv(rolesCommon.envRoles.deploymentManager, false, defaultApplicationName);
  });

  it('should be able to navigate as an application deployment manager in the application if user is in a group which has this right', function() {
    toggleGroupRoleForEnv(rolesCommon.envRoles.deploymentManager, true, defaultApplicationName);
    checkApplicationDeploymentManagerAccess(defaultApplicationName);
    toggleGroupRoleForEnv(rolesCommon.envRoles.deploymentManager, false, defaultApplicationName);
  });

  it('should be able to navigate as an application dev ops in the application if user has this right', function() {
    toggleUserRole(rolesCommon.appRoles.appDevops, true, defaultApplicationName);
    checkApplicationDevOpsAccess(defaultApplicationName);
    toggleUserRole(rolesCommon.appRoles.appDevops, false, defaultApplicationName);
  });

  it('should be able to navigate as an application dev ops in the application if user is in a group which has this right', function() {
    toggleGroupRole(rolesCommon.appRoles.appDevops, true, defaultApplicationName);
    checkApplicationDevOpsAccess(defaultApplicationName);
    toggleGroupRole(rolesCommon.appRoles.appDevops, false, defaultApplicationName);
  });

  it('should be able to navigate as an application user in the application if user has this right', function() {
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, true, defaultApplicationName);
    checkApplicationUserAccess(defaultApplicationName);
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, false, defaultApplicationName);
  });

  it('should be able to navigate as an application user in the application if user is in a group which has this right', function() {
    toggleGroupRoleForEnv(rolesCommon.envRoles.envUser, true, defaultApplicationName);
    checkApplicationUserAccess(defaultApplicationName);
    toggleGroupRoleForEnv(rolesCommon.envRoles.envUser, false, defaultApplicationName);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
