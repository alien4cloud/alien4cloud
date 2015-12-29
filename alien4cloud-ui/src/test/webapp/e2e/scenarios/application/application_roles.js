'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var users = require('../../admin/users');
var rolesCommon = require('../../common/roles_common');
var applications = require('../../applications/applications');


describe('Security management on applications', function() {

  var applicationName = 'AlienUITest';

  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  var checkAccess = function(menu) {
    common.isNavigable('applications', menu);
    common.go('applications', menu);
    toaster.expectNoErrors();
  };

  var checkDisplayedButDisabled = function(menu) {
    common.isPresentButDisabled('applications', menu);
  };

  var checkApplicationManagerAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    checkAccess('topology');
    checkAccess('deployment');
    checkDisplayedButDisabled('runtime');
    checkAccess('users');
    checkAccess('info');
    authentication.reLogin('admin');

  };

  var checkApplicationDeploymentManagerAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);

    // It must has access to every tab of the application
    checkAccess('info');
    checkAccess('deployment');
    checkDisplayedButDisabled('runtime');
    common.isNotNavigable('applications', 'topology');
    common.isNotNavigable('applications', 'users');
    authentication.reLogin('admin');
  };

  var checkApplicationDevOpsAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    // It must has access to every tab of the application
    checkAccess('topology');
    common.isNotNavigable('applications', 'deployment');
    common.isNotNavigable('applications', 'runtime');
    common.isNotNavigable('applications', 'users');
    checkAccess('info');
    authentication.reLogin('admin');
  };

  var checkApplicationUserAccess = function() {
    authentication.reLogin(authentication.users.sauron.username);
    applications.goToApplicationDetailPage(applicationName);
    // It must has access to every tab of the application
    common.isNotNavigable('applications', 'topology');
    common.isNotNavigable('applications', 'deployment');
    common.isNotNavigable('applications', 'runtime');
    common.isNotNavigable('applications', 'users');
    checkAccess('info');
    authentication.reLogin('admin');
  };

  var toggleUserRole = function(role) {
    applications.goToApplicationDetailPage(applicationName);
    common.go('applications', 'users');
    rolesCommon.editUserRole(authentication.users.sauron.username, role);
  };

  var toggleGroupRole = function(role) {
    applications.goToApplicationDetailPage(applicationName);
    common.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRole(users.groups.mordor.name, role);
  };

  var toggleUserRoleForEnv = function(role) {
    applications.goToApplicationDetailPage(applicationName);
    common.go('applications', 'users');
    rolesCommon.editUserRoleForEnv(authentication.users.sauron.username, role);
  };

  var toggleGroupRoleForEnv = function(role) {
    applications.goToApplicationDetailPage(applicationName);
    common.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();
    rolesCommon.editGroupRoleForEnv(users.groups.mordor.name, role);
  };

  it('should be able to navigate as an application manager in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application manager in the application if user has this right');
    toggleUserRole(rolesCommon.appRoles.appManager);
    checkApplicationManagerAccess();
    toggleUserRole(rolesCommon.appRoles.appManager);
  });

  it('should be able to navigate as an application manager in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application manager in the application if user is in a group which has this right');
    toggleGroupRole(rolesCommon.appRoles.appManager);
    checkApplicationManagerAccess();
    toggleGroupRole(rolesCommon.appRoles.appManager);
  });

  it('should be able to navigate as an application deployment manager in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application deployment manager in the application if user has this right');
    toggleUserRoleForEnv(rolesCommon.envRoles.deploymentManager);
    checkApplicationDeploymentManagerAccess();
    toggleUserRoleForEnv(rolesCommon.envRoles.deploymentManager);
  });

  it('should be able to navigate as an application deployment manager in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application deployment manager in the application if user is in a group which has this right');
    toggleGroupRoleForEnv(rolesCommon.envRoles.deploymentManager);
    checkApplicationDeploymentManagerAccess();
    toggleGroupRoleForEnv(rolesCommon.envRoles.deploymentManager);
  });

  it('should be able to navigate as an application dev ops in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application dev ops in the application if user has this right');
    toggleUserRole(rolesCommon.appRoles.appDevops);
    checkApplicationDevOpsAccess();
    toggleUserRole(rolesCommon.appRoles.appDevops);
  });

  it('should be able to navigate as an application dev ops in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application dev ops in the application if user is in a group which has this right');
    toggleGroupRole(rolesCommon.appRoles.appDevops);
    checkApplicationDevOpsAccess();
    toggleGroupRole(rolesCommon.appRoles.appDevops);
  });

  it('should be able to navigate as an application user in the application if user has this right', function() {
    console.log('################# should be able to navigate as an application user in the application if user has this right');
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser);
    checkApplicationUserAccess();
    toggleUserRoleForEnv(rolesCommon.envRoles.envUser);
  });

  it('should be able to navigate as an application user in the application if user is in a group which has this right', function() {
    console.log('################# should be able to navigate as an application user in the application if user is in a group which has this right');
    toggleGroupRoleForEnv(rolesCommon.envRoles.envUser);
    checkApplicationUserAccess();
    toggleGroupRoleForEnv(rolesCommon.envRoles.envUser);
  });
});
