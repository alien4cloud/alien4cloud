/* global by */

'use strict';

var common = require('../../common/common');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var cloudsCommon = require('../../admin/clouds_common');
var rolesCommon = require('../../common/roles_common');
var componentData = require('../../topology/component_data');

describe('Topology input/output properties', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  afterEach(function() {
    common.after();
  });

  var checkPropertyState = function() {
    topologyEditorCommon.expectPropertyInputState('Compute', 'ip_address', true);
    topologyEditorCommon.expectPropertyOutputState('Compute', 'ip_address', false);
    topologyEditorCommon.expectPropertyOutputState('Compute', 'disk_size', true);
    topologyEditorCommon.expectPropertyInputState('Compute', 'disk_size', false);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
  };

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
    topologyEditorCommon.editNodeProperty('Compute', 'disk_size', '1024');
    topologyEditorCommon.editNodeProperty('Compute', 'ip_address', '192.168.1.1');

    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);

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
    browser.sleep(7000); // DO NOT REMOVE, output visible few seconds after DEPLOY click

    var outputTable = browser.element(by.id('outputPropertiesTable'));
    var outputTableText = outputTable.getText();
    expect(outputTableText).toContain('disk_size');
    expect(outputTableText).toContain('1024');
    expect(outputTableText).toContain('ip_address');
    expect(outputTableText).toContain('10.52.0.');


    var inputTable = browser.element(by.id('inputPropertiesTable'));
    var inputTableText = inputTable.getText();
    expect(inputTableText).toContain('ip_address');
    expect(inputTableText).toContain('192.168.1.1');

    var undeployButton = browser.element(by.binding('APPLICATIONS.UNDEPLOY'));
    browser.actions().click(undeployButton).perform();
    browser.sleep(7000); // DO NOT REMOVE, wait for UNDEPLOY
    outputTableText = outputTable.getText();
    expect(outputTableText).not.toContain('disk_size');
    expect(outputTableText).not.toContain('1024');
    expect(outputTableText).not.toContain('ip_address');
    expect(outputTableText).not.toContain('10.52.0.');

  });
});
