/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');
var cloudsCommon = require('../../admin/clouds_common');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');
var rolesCommon = require('../../common/roles_common');

function assertCountEnvironment(expectedCount) {
  var environments = element.all(by.repeater('environment in searchAppEnvResult'));
  expect(environments.count()).toEqual(expectedCount);
}

describe('Application environments security check', function() {

  // var reset = false;
  // var after = false;

  /* Before each spec in the tests suite */
  beforeEach(function() {

    topologyEditorCommon.beforeTopologyTest();

    // if (reset === true) {
    //   // clean indexes
    //   common.before();
    //   // create and configure a cloud
    //   authentication.login('admin');
    //   cloudsCommon.goToCloudList();
    //   cloudsCommon.createNewCloud('mock');
    //   cloudsCommon.goToCloudDetail('mock');
    //   cloudsCommon.enableCloud();
    //
    //   // authentication.reLogin('applicationManager');
    //   // cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    //   applications.createApplication('Alien', 'Great Application with application environment to perform deployments...');
    //   // applications.goToApplicationEnvironmentPageForApp('Alien');
    // }
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    // if(after) {
      common.after();
    // }
  });

  it('should be able to deploy on default Environment as APPLICATION_MANAGER', function() {
    console.log('################# should be able to deploy on default Environment as APPLICATION_MANAGER');
    // assertCountEnvironment(1);
    // expect(element(by.id('Environment-envlistid')).$('option:checked').getText()).toEqual('OTHER');

    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'topology');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      compute: componentData.toscaBaseTypes.compute()
    });
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'disk_size', '1024');
    // topologyEditorCommon.editNodeProperty('Compute', 'ip_address', '192.168.1.1');

    applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);

    browser.sleep(10000);

    navigation.go('applications', 'info');

    browser.sleep(10000);

  });

  xit('description asynchrone', function(done) {
    // body...
    console.log(' asynchrone DONE>', done);
  });

  xit('description synchrone', function() {
    // body...
    console.log(' synchrone');
  });

  // it('should create an application and must have a default application environment.', function() {
  //   console.log('################# should create an application and must have a default application environment.');
  //   assertCountEnvironment(1);
  //   expect(element(by.id('Environment-envlistid')).$('option:checked').getText()).toEqual('OTHER');
  // });
  //
  // it('should create an application environment for a new application.', function() {
  //   console.log('################# should create an application environment for a new application.');
  //   applications.createApplicationEnvironment('ENV', 'A new environment for my application...', 'testcloud', applications.environmentTypes.dev, '0.1.0-SNAPSHOT');
  //   common.expectNoErrors();
  //   assertCountEnvironment(2);
  // });
  //
  // it('should be able to delete an application environment.', function() {
  //   console.log('################# should be able to delete a created environment.');
  //   assertCountEnvironment(2);
  //   common.deleteWithConfirm('delete-env_ENV', true);
  //   common.expectNoErrors();
  //   assertCountEnvironment(1);
  // });
  //
  // it('should reject a new application environment if an application environment with the same name already exist.', function() {
  //   console.log('################# should reject a new application environment if an application environment with the same name already exist.');
  //   applications.createApplicationEnvironment('Environment', 'A new environment whith an existing name', 'testcloud', applications.environmentTypes.dev, '0.1.0-SNAPSHOT');
  //   common.expectErrors();
  //   common.dismissAlert();
  //   assertCountEnvironment(1);
  // });
  //
  // it('should failed to remove the last new application environment.', function() {
  //   console.log('################# should failed to remove the last new application environment.');
  //   common.deleteWithConfirm('delete-env_Environment', true);
  //   common.expectErrors();
  //   common.dismissAlert();
  //   assertCountEnvironment(1);
  // });
  //
  // it('should failed to rename if an application environment with the same name already exist.', function() {
  //   after = true;
  //   console.log('################# should failed to rename if an application environment with the same name already exist.');
  //   applications.createApplicationEnvironment('ENV', 'A new environment for my application...', 'testcloud', applications.environmentTypes.dev, '0.1.0-SNAPSHOT');
  //   common.expectNoErrors();
  //   assertCountEnvironment(2);
  //   common.sendValueToXEditable('ENV-name-td', 'Environment', false);
  //   common.expectErrors();
  //   browser.sleep(5000); // DO NOT REMOVE, we need to send a valid value to the editable text
  //   element(by.css('#ENV-name-td input')).sendKeys('2');
  // });

});
