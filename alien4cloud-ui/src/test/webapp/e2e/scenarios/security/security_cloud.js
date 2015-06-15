/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var authentication = require('../../authentication/authentication');
var applications = require('../../applications/applications');
var cloudsCommon = require('../../admin/clouds_common');
var rolesCommon = require('../../common/roles_common.js');
var topologyTemplates = require('../../topology/topology_templates_common');
var navigation = require('../../common/navigation');
var users = require('../../admin/users');

function assertUserOrGroupHasCloudRole(roleRepeater, targetedRole, userOrGroupName) {
  roleRepeater.each(function(cloudRole) {
    cloudRole.getText().then(function(cloudBloc) {
      if (cloudBloc.indexOf(targetedRole) > -1) {
        expect(cloudBloc).toContain(userOrGroupName);
      }
    });
  });
}

function createSimpleApp() {

  // create a simple app as admin
  authentication.reLogin('admin');

  // create a topology template with one Compute, 1 Java and 1 Tomcat RPM
  topologyTemplates.createTopologyTemplateWithNodesAndRelationships(topologyEditorCommon.topologyTemplates.template4);

  // create a new application with this template as model
  authentication.reLogin('applicationManager');
  // template at index 2 is the first and only one created (index 1 equals is the default, no template)
  applications.createApplication('JavaTomcatWarApplication', 'Simple application with one compute, one template and all that around java...', 2);

  // check the created topology application (count nodes, count relationship..)
  // i should not be able to see the cloud in the list
  applications.goToApplicationDetailPage('JavaTomcatWarApplication', true);

}

function assertUserOrGroupHasRoleOnCloud(boolHasRole) {
  // now applicationManager has no right CLOUD_DEPLOYER
  authentication.reLogin('applicationManager');
  applications.goToApplicationDetailPage('JavaTomcatWarApplication', true);
  navigation.go('applications', 'deployment');
  var countPlugin = cloudsCommon.selectDeploymentPluginCount();
  var selected = cloudsCommon.selectApplicationCloud('testcloud');
  if (boolHasRole === true) {
    expect(countPlugin).toBe(2); // default -- CLOUD -- AND testcloud
    expect(selected).toBe(true); // testcloud is in the select
  } else {
    expect(countPlugin).toBe(1); // only -- CLOUD --
    expect(selected).toBe(false); // testcloud is in not the select
  }
}

describe('Cloud security and deployment capability per user/group', function() {

  beforeEach(function() {
    cloudsCommon.beforeWithCloud();
    // only to have applicationManager user active in Alien
    authentication.login('applicationManager');
  });

  afterEach(function() {
    common.after();
  });

  it('should add rights to some users on a cloud without error', function() {

    console.log('################# should add rights to some users on a cloud without error');

    // give rights to applicationManager and admin on the cloud
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'admin', rolesCommon.cloudRoles.cloudDeployer);

    // check that the user is well mentioned in the right role bucket
    var cloudRoles = element.all(by.repeater('cloudRole in cloudRoles'));
    assertUserOrGroupHasCloudRole(cloudRoles, rolesCommon.cloudRoles.cloudDeployer, '(applicationManager)');
    assertUserOrGroupHasCloudRole(cloudRoles, rolesCommon.cloudRoles.cloudDeployer, '(admin)');

  });

  it('should add rights to a user and check that he can use this cloud', function() {
    console.log('################# should add rights to a user in check that he can use this cloud');

    createSimpleApp();

    // give rights to applicationManager on the cloud
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);

    // checks
    assertUserOrGroupHasRoleOnCloud(true);

    // Remove right for this unser in this cloud (checked previously)
    authentication.reLogin('admin');
    // toggle grant/remove => here remove
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);

    // checks
    assertUserOrGroupHasRoleOnCloud(false);

  });


  it('should add rights to some groups on a cloud without error', function() {

    console.log('################# should add rights to some groups on a cloud without error');

    // first create 2 groups
    authentication.reLogin('admin');

    // create and check a group
    users.createGroup(users.groups.managers);
    users.createGroup(users.groups.architects);

    // give rights to Architects and Managers groups on the cloud
    cloudsCommon.giveRightsOnCloudToGroup('testcloud', users.groups.managers.name, rolesCommon.cloudRoles.cloudDeployer);
    cloudsCommon.giveRightsOnCloudToGroup('testcloud', users.groups.architects.name, rolesCommon.cloudRoles.cloudDeployer);

    var cloudRoles = element.all(by.repeater('cloudRole in cloudRoles'));
    assertUserOrGroupHasCloudRole(cloudRoles, rolesCommon.cloudRoles.cloudDeployer, 'Architects');
    assertUserOrGroupHasCloudRole(cloudRoles, rolesCommon.cloudRoles.cloudDeployer, 'Managers');

  });


  it('should add rights to a group and check that user from this group can use this cloud', function() {
    console.log('################# should add rights to a group and check that user from this group can use this cloud');

    createSimpleApp();

    // first create 1 group
    authentication.reLogin('admin');
    // create and check a group
    users.createGroup(users.groups.managers);

    // give rights to Architects and Managers groups on the cloud
    cloudsCommon.giveRightsOnCloudToGroup('testcloud', users.groups.managers.name, rolesCommon.cloudRoles.cloudDeployer);

    // add applicationManager user to one group
    users.navigationUsers();
    rolesCommon.addUserToGroup('applicationManager', users.groups.managers.name);

    // checks
    assertUserOrGroupHasRoleOnCloud(true);

    // Remove the user from this group and check that he does not have access to the cloud

    // remove applicationManager user to one group
    authentication.reLogin('admin');
    users.navigationUsers();
    rolesCommon.removeUserFromGroup('applicationManager', users.groups.managers.name);

    // checks
    assertUserOrGroupHasRoleOnCloud(false);

  });


  it('should add rights to a group and to a user from this group to check rights', function() {
    console.log('################# should add rights to a group and to a user from this group to check rights');

    createSimpleApp();

    // first create 2 groups
    authentication.reLogin('admin');

    // create and check a group
    users.createGroup(users.groups.architects);

    // add applicationManager user to architects groups
    users.navigationUsers();
    rolesCommon.addUserToGroup('applicationManager', users.groups.architects.name);

    // check : at start no rights for any user or cloud
    assertUserOrGroupHasRoleOnCloud(false);

    // give rights to Architects on 'testcloud'
    cloudsCommon.giveRightsOnCloudToGroup('testcloud', users.groups.architects.name, rolesCommon.cloudRoles.cloudDeployer);

    // give rights to applicationManager on 'testcloud'
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);

    // remove applicationManager from Architects group
    authentication.reLogin('admin');
    users.navigationUsers();
    rolesCommon.removeUserFromGroup('applicationManager', users.groups.architects.name);

    // check that applicationManager user still have his own CLOUD_DEPLOYER role on this cloud
    assertUserOrGroupHasRoleOnCloud(true);

    // Remove right for this user in this cloud (checked previously)
    authentication.reLogin('admin');
    // toggle grant/remove => here remove
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);

    // checks
    assertUserOrGroupHasRoleOnCloud(false);

  });

  it('should add right ALL_USERS group on a cloud and check that any user can use this cloud', function() {
    console.log('################# should add right to ALL_USERS group on a cloud and check that any user can use this cloud');

    // first create 1 group
    authentication.reLogin('admin');

    users.navigationUsers();
    users.createUser(authentication.users.sauron);

    // create an app Alien
    applications.createApplication('Alien', 'Great Application');

    applications.goToApplicationDetailPage('Alien');
    navigation.go('applications', 'users');
    element(by.id('groups-tab')).element(by.tagName('a')).click();

    // give appManager role to group ALL_USERS
    rolesCommon.editGroupRole('ALL_USERS', rolesCommon.appRoles.appManager);

    // give rights to ALL_USERS group on the cloud
    cloudsCommon.giveRightsOnCloudToGroup('testcloud', users.groups.allusers.name, rolesCommon.cloudRoles.cloudDeployer);

    // deploy an applicaton as sauron
    // TODO : find workaround, working in application but not in protractor tests (left submenu not present)
    // authentication.reLogin(authentication.users.sauron.username);
    // applications.goToApplicationListPage();
    // applications.goToApplicationDetailPage('Alien', false);
    // navigation.go('applications', 'deployment');
    // var selected = cloudsCommon.selectApplicationCloud('testcloud');
    // expect(selected).toBe(true); // testcloud is in the select

  });

});
