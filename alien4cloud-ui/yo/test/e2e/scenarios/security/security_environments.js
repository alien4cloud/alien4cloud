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

  /* Before each spec in the tests suite */
  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to deploy on default Environment and check output properties / attributes on deployment / info page', function() {
    console.log('################# should be able to deploy on default Environment and check output properties / attributes on deployment / info page');

    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'topology');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      compute: componentData.toscaBaseTypes.compute()
    });
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    topologyEditorCommon.editNodeProperty('Compute', 'disk_size', '1024');

    // check properties / attributes as output
    topologyEditorCommon.togglePropertyOutput('Compute', 'disk_size');
    topologyEditorCommon.togglePropertyOutput('Compute', 'os_type');
    topologyEditorCommon.toggleAttributeOutput('Compute', 'ip_address');
    topologyEditorCommon.toggleAttributeOutput('Compute', 'tosca_name');

    // check after toggle
    topologyEditorCommon.expectPropertyOutputState('Compute', 'os_arch', false);
    topologyEditorCommon.expectPropertyOutputState('Compute', 'disk_size', true);
    topologyEditorCommon.expectPropertyOutputState('Compute', 'os_type', true);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'tosca_name', true);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'tosca_id', false);

    // Deploy the app
    applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);

    // checking deployment page
    applications.expectOutputValue('deployment', null, 'attribute', 'Compute', 1, 'ip_address', '10.52.0.1');
    applications.expectOutputValue('deployment', null, 'attribute', 'Compute', 1, 'tosca_name', 'TOSCA-Simple-Profile-YAML');
    applications.expectOutputValue('deployment', null, 'property', 'Compute', 1, 'disk_size', '1024');
    applications.expectOutputValue('deployment', null, 'property', 'Compute', 1, 'os_type', 'windows');

    // check on info page
    applications.expectOutputValue('info', null, 'attribute', 'Compute', 1, 'ip_address', '10.52.0.1');
    applications.expectOutputValue('info', null, 'attribute', 'Compute', 1, 'tosca_name', 'TOSCA-Simple-Profile-YAML');
    applications.expectOutputValue('info', null, 'property', 'Compute', 1, 'disk_size', '1024');
    applications.expectOutputValue('info', null, 'property', 'Compute', 1, 'os_type', 'windows');

  });

  xit('should be able to deploy 2 environments with 2 differents version and check outputs', function() {
    console.log('################# should be able to deploy 2 environments with 2 differents version and check outputs');


    // create a new version
    applications.createApplicationVersion('0.2.0-SNAPSHOT', 'A new version for my application...', '0.1.0-SNAPSHOT');
    common.expectNoErrors();

    // create new version and new environment based on this
    applications.createApplicationEnvironment('DEV', 'A new dev environment for my application...', 'testcloud', applications.environmentTypes.dev, '0.2.0-SNAPSHOT');
    common.expectNoErrors();
    assertCountEnvironment(2); // DEV + ENvironment

    // select version 0.1.0-SNAPSHOT
    // navigation.go('applications', targetedPageStateId);


    // select version 0.2.0-SNAPSHOT

    // cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    // navigation.go('main', 'applications');
    // browser.element(by.binding('application.name')).click();
    // navigation.go('applications', 'topology');
    // topologyEditorCommon.addNodeTemplatesCenterAndZoom({
    //   compute: componentData.toscaBaseTypes.compute()
    // });
    // topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    // topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    // topologyEditorCommon.editNodeProperty('Compute', 'disk_size', '1024');
    //
    // // check properties / attributes as output
    // topologyEditorCommon.togglePropertyOutput('Compute', 'disk_size');
    // topologyEditorCommon.togglePropertyOutput('Compute', 'os_type');
    // topologyEditorCommon.toggleAttributeOutput('Compute', 'ip_address');
    // topologyEditorCommon.toggleAttributeOutput('Compute', 'tosca_name');
    //
    // // check after toggle
    // topologyEditorCommon.expectPropertyOutputState('Compute', 'os_arch', false);
    // topologyEditorCommon.expectPropertyOutputState('Compute', 'disk_size', true);
    // topologyEditorCommon.expectPropertyOutputState('Compute', 'os_type', true);
    // topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
    // topologyEditorCommon.expectAttributeOutputState('Compute', 'tosca_name', true);
    // topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
    // topologyEditorCommon.expectAttributeOutputState('Compute', 'tosca_id', false);
    //
    // // Deploy the app
    // applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);
    //
    // // checking deployment page
    // applications.expectOutputValue('deployment', null, 'attribute', 'Compute', 1, 'ip_address', '10.52.0.1');
    // applications.expectOutputValue('deployment', null, 'attribute', 'Compute', 1, 'tosca_name', 'TOSCA-Simple-Profile-YAML');
    // applications.expectOutputValue('deployment', null, 'property', 'Compute', 1, 'disk_size', '1024');
    // applications.expectOutputValue('deployment', null, 'property', 'Compute', 1, 'os_type', 'windows');
    //
    // // check on info page
    // applications.expectOutputValue('info', null, 'attribute', 'Compute', 1, 'ip_address', '10.52.0.1');
    // applications.expectOutputValue('info', null, 'attribute', 'Compute', 1, 'tosca_name', 'TOSCA-Simple-Profile-YAML');
    // applications.expectOutputValue('info', null, 'property', 'Compute', 1, 'disk_size', '1024');
    // applications.expectOutputValue('info', null, 'property', 'Compute', 1, 'os_type', 'windows');

  });

  xit('should not be able to see outputs of a deployed environment without rights on a cloud', function() {
    console.log('################# should not be able to see outputs of a deployed environment without rights on a cloud');
  });

  xit('should should not be able to deploy an environment without right on the underlying cloud', function() {
    console.log('################# should should not be able to deploy an environment without right on the underlying cloud');
  });

});
