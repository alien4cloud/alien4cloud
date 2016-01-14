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

  var applicationName = 'AlienUITest';
  var otherApplicationName = 'ApplicationRolesTestOtherApp';

  var checkAccess = function(menu) {
    common.isNavigable('applications', menu);
    common.go('applications', menu);
    toaster.expectNoErrors();
  };

  var checkDisplayedButDisabled = function(menu) {
    common.isPresentButDisabled('applications', menu);
  };

  var checkApplicationManagerAccess = function(specifiedApp) {
    authentication.reLogin(authentication.users.sauron.username);
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
    checkAccess('topology');
    checkAccess('deployment');
    checkDisplayedButDisabled('runtime');
    checkAccess('users');
    checkAccess('info');
    authentication.reLogin('applicationManager');
  };

  var checkApplicationDeploymentManagerAccess = function(specifiedApp) {
    authentication.reLogin(authentication.users.sauron.username);
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
    // It must has access to every tab of the application
    checkAccess('info');
    checkAccess('deployment');
    checkDisplayedButDisabled('runtime');
    common.isNotNavigable('applications', 'topology');
    common.isNotNavigable('applications', 'users');
    authentication.reLogin('applicationManager');
  };

  var checkApplicationDevOpsAccess = function(specifiedApp) {
    authentication.reLogin(authentication.users.sauron.username);
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
    // It must has access to every tab of the application
    checkAccess('topology');
    common.isNotNavigable('applications', 'deployment');
    common.isNotNavigable('applications', 'runtime');
    common.isNotNavigable('applications', 'users');
    checkAccess('info');
    authentication.reLogin('applicationManager');
  };

  var checkApplicationUserAccess = function(specifiedApp) {
    authentication.reLogin(authentication.users.sauron.username);
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
    // It must has access to every tab of the application
    common.isNotNavigable('applications', 'topology');
    common.isNotNavigable('applications', 'deployment');
    common.isNotNavigable('applications', 'runtime');
    common.isNotNavigable('applications', 'users');
    checkAccess('info');
    authentication.reLogin('applicationManager');
  };

  var checkNoAccessToApp = function(specifiedApp) {
    authentication.reLogin(authentication.users.sauron.username);
    if (!specifiedApp) {
      specifiedApp = applicationName;
    }
    applications.go();
    applications.searchApplication(specifiedApp);
    expect(element(by.id('app_' + specifiedApp)).isPresent()).toBe(false);
    authentication.reLogin('applicationManager');
  };

  var toggleUserRole = function(role, hasRole, specifiedApp) {
    applications.go();
    if (specifiedApp) {
      applications.searchApplication(specifiedApp);
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.searchApplication(applicationName);
      applications.goToApplicationDetailPage(applicationName);
    }
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

  var toggleGroupRole = function(role, hasRole, specifiedApp) {
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
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

  var toggleUserRoleForEnv = function(role, hasRole, specifiedApp) {
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
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

  var toggleGroupRoleForEnv = function(role, hasRole, specifiedApp) {
    if (specifiedApp) {
      applications.goToApplicationDetailPage(specifiedApp);
    } else {
      applications.goToApplicationDetailPage(applicationName);
    }
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
    toggleUserRole(rolesCommon.appRoles.appManager, true);
    checkApplicationManagerAccess();
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, true, otherApplicationName);
    checkApplicationUserAccess(otherApplicationName);
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, false, otherApplicationName);
    toggleUserRole(rolesCommon.appRoles.appManager, false);
  });

  it('should be able to navigate as an application manager in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application manager in the application if user has this right');
    toggleUserRole(rolesCommon.appRoles.appManager, true);
    checkApplicationManagerAccess();
    toggleUserRole(rolesCommon.appRoles.appManager, false);
    checkNoAccessToApp();
  });

  it('should be able to navigate as an application manager in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application manager in the application if user is in a group which has this right');
    toggleGroupRole(rolesCommon.appRoles.appManager, true);
    checkApplicationManagerAccess();
    toggleGroupRole(rolesCommon.appRoles.appManager, false);
  });

  it('should be able to navigate as an application deployment manager in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application deployment manager in the application if user has this right');
    toggleUserRoleForEnv(rolesCommon.envRoles.deploymentManager, true);
    checkApplicationDeploymentManagerAccess();
    toggleUserRoleForEnv(rolesCommon.envRoles.deploymentManager, false);
  });

  it('should be able to navigate as an application deployment manager in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application deployment manager in the application if user is in a group which has this right');
    toggleGroupRoleForEnv(rolesCommon.envRoles.deploymentManager, true);
    checkApplicationDeploymentManagerAccess();
    toggleGroupRoleForEnv(rolesCommon.envRoles.deploymentManager, false);
  });

  it('should be able to navigate as an application dev ops in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application dev ops in the application if user has this right');
    toggleUserRole(rolesCommon.appRoles.appDevops, true);
    checkApplicationDevOpsAccess();
    toggleUserRole(rolesCommon.appRoles.appDevops, false);
  });

  it('should be able to navigate as an application dev ops in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application dev ops in the application if user is in a group which has this right');
    toggleGroupRole(rolesCommon.appRoles.appDevops, true);
    checkApplicationDevOpsAccess();
    toggleGroupRole(rolesCommon.appRoles.appDevops, false);
  });

  it('should be able to navigate as an application user in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application user in the application if user has this right');
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, true);
    checkApplicationUserAccess();
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser, false);
  });

  it('should be able to navigate as an application user in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application user in the application if user is in a group which has this right');
    toggleGroupRoleForEnv(rolesCommon.envRoles.envUser, true);
    checkApplicationUserAccess();
    toggleGroupRoleForEnv(rolesCommon.envRoles.envUser, false);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
