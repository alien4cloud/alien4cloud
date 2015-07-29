/* global by */

'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var rolesCommon = require('../../common/roles_common');
var componentData = require('../../topology/component_data');
var tagConfigCommon = require('../../admin/metaprops_configuration_common');
var cloudsCommon = require('../../admin/clouds_common');
var applications = require('../../applications/applications');

describe('Topology input/output properties', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  afterEach(function() {
    authentication.logout();
  });

  var checkPropertyState = function() {
    browser.sleep(1000);
    topologyEditorCommon.expectPropertyInputState('Compute', 'ip_address', true);
    topologyEditorCommon.expectPropertyOutputState('Compute', 'ip_address', false);
    topologyEditorCommon.expectPropertyOutputState('Compute', 'disk_size', true);
    topologyEditorCommon.expectPropertyInputState('Compute', 'disk_size', false);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
  };

  /* test to ensure an non-regresion on ALIEN-1061 */
  it('should be able to define properties as input of cloud meta and update the view when we change the cloud', function() {
    console.log('################# should be able to define properties as input of cloud meta and update the view when we change the cloud');
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'topology');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      compute: componentData.toscaBaseTypes.compute()
    });
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    topologyEditorCommon.togglePropertyInput('Compute', 'os_distribution');
    topologyEditorCommon.renameInput('os_distribution', 'cloud_meta_distribution');

    // create 2 clouds and add a meta_cloud property
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud-meta-empty');
    cloudsCommon.goToCloudDetail('testcloud-meta-empty');
    cloudsCommon.enableCloud();
    tagConfigCommon.addTagConfiguration(tagConfigCommon.defaultCloudProperty, null);

    // set a value in the cloud_meta_distribution of testcloud
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.goToCloudConfiguration();
    cloudsCommon.showMetaProperties();
    expect(element(by.id('cloudMetaPropertiesDisplay')).isDisplayed()).toBe(true);
    tagConfigCommon.editTagConfiguration('distribution', 'debian');

    // now we go in deploymet page and change the cloud to test the input cloud meta validation
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'deployment');
    applications.switchEnvironmentAndCloud(null, 'testcloud');
    topologyEditorCommon.checkWarningList(false);
    applications.switchEnvironmentAndCloud(null, 'testcloud-meta-empty');
    topologyEditorCommon.checkWarningList(true);
  });

  it('should be able to define properties as input or output and see their values in application details view', function() {
    console.log('################# should be able to define properties as input or output and see their values in application details view');
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
    topologyEditorCommon.editNodeProperty('Compute', 'disk_size', '1024', 'pro', 'MIB');
    topologyEditorCommon.editNodeProperty('Compute', 'ip_address', '192.168.1.1');

    topologyEditorCommon.addScalingPolicy('Compute', 1, 2, 3);

    topologyEditorCommon.togglePropertyInput('Compute', 'ip_address');
    topologyEditorCommon.togglePropertyOutput('Compute', 'disk_size');
    topologyEditorCommon.toggleAttributeOutput('Compute', 'ip_address');
    checkPropertyState();

    // check again after reloading the page
    navigation.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'topology');
    checkPropertyState();

    navigation.go('applications', 'deployment');
    var selected = cloudsCommon.selectApplicationCloud('testcloud');
    expect(selected).toBe(true); // testcloud is in the select
    browser.sleep(1000);
    var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
    browser.actions().click(deployButton).perform();
    browser.sleep(9000); // DO NOT REMOVE, output visible few seconds after DEPLOY click

    var outputTable = browser.element(by.id('outputPropertiesTable'));
    var outputTableText = outputTable.getText();
    expect(outputTableText).toContain('disk_size');
    expect(outputTableText).toContain('1024');
    expect(outputTableText).toContain('ip_address');
    expect(outputTableText).toContain('10.52.0.');


    var inputTable = browser.element(by.id('inputsTable'));
    var inputTableText = inputTable.getText();
    expect(inputTableText).toContain('ip_address');
    expect(inputTableText).toContain('');

    var undeployButton = browser.element(by.binding('APPLICATIONS.UNDEPLOY'));
    browser.sleep(1000);
    browser.actions().click(undeployButton).perform();
    browser.sleep(9000); // DO NOT REMOVE, wait for UNDEPLOY
    outputTableText = outputTable.getText();
    expect(outputTableText).not.toContain('disk_size');
    expect(outputTableText).not.toContain('1024');
    expect(outputTableText).not.toContain('ip_address');
    expect(outputTableText).not.toContain('10.52.0.');
  });
});
