/* global by */

'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
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

  it('should be able to define properties as input or output and see their values in application details view', function() {
    console.log('################# should be able to define properties as input or output and see their values in application details view');
    common.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    common.go('applications', 'topology');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      compute: componentData.toscaBaseTypes.compute()
    });
    topologyEditorCommon.editNodeProperty('Compute', 'architecture', 'x86_64', 'cap');
    topologyEditorCommon.editNodeProperty('Compute', 'distribution', 'Ubuntu', 'cap');
    topologyEditorCommon.editNodeProperty('Java', 'component_version', '1.8');
    topologyEditorCommon.editNodeProperty('Java', 'java_home', '/root/java');

    topologyEditorCommon.addScalingPolicy('Compute', 1, 2, 3);

    topologyEditorCommon.togglePropertyInput('Compute', 'architecture');
    topologyEditorCommon.togglePropertyOutput('Compute', 'distribution');
    topologyEditorCommon.toggleAttributeOutput('Compute', 'ip_address');
    checkPropertyState();

    // check again after reloading the page
    common.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    common.go('applications', 'topology');
    checkPropertyState();

    common.go('applications', 'deployment');
    var selected = cloudsCommon.selectApplicationCloud('testcloud');
    expect(selected).toBe(true); // testcloud is in the select
    browser.sleep(2000);
    var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
    browser.actions().click(deployButton).perform();
    browser.sleep(13000); // DO NOT REMOVE, output visible few seconds after DEPLOY click

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
    browser.sleep(2000);
    browser.actions().click(undeployButton).perform();
    browser.sleep(13000); // DO NOT REMOVE, wait for UNDEPLOY
    outputTableText = outputTable.getText();
    expect(outputTableText).not.toContain('disk_size');
    expect(outputTableText).not.toContain('1024');
    expect(outputTableText).not.toContain('ip_address');
    expect(outputTableText).not.toContain('10.52.0.');
  });
});
