/* global describe, it, by, expect, browser */
'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var applications = require('../../applications/applications');
var topologiesData = require(__dirname + '/_data/application_topology_editor_input_output/topologies.json');
var deploymentTopologiesData = require(__dirname + '/_data/application_topology_editor_input_output/deploymenttopologies.json');

describe('Topology input/output properties', function() {

  var checkPropertyState = function() {
    topologyEditorCommon.expectPropertyInputState('Compute', 'architecture', true, 'cap');
    topologyEditorCommon.expectPropertyInputState('Compute', 'type', false, 'cap');

    topologyEditorCommon.expectPropertyOutputState('Compute', 'distribution', true, 'cap');
    topologyEditorCommon.expectPropertyOutputState('Compute', 'distribution', true, 'cap');
    topologyEditorCommon.expectPropertyOutputState('Compute', 'type', false, 'cap');

    topologyEditorCommon.expectPropertyInputState('Java', 'component_version', true);
    topologyEditorCommon.expectPropertyInputState('Java', 'java_url', false);

    topologyEditorCommon.expectPropertyOutputState('Java', 'java_home', true);
    topologyEditorCommon.expectPropertyOutputState('Java', 'java_url', false);

    topologyEditorCommon.expectAttributeOutputState('Compute', 'ip_address', true);
    topologyEditorCommon.expectAttributeOutputState('Compute', 'public_ip_address', false);
  };

  it('beforeAll', function() {
    setup.setup();
    setup.index('topology', 'topology', topologiesData);
    setup.index('deploymenttopology', 'deploymenttopology', deploymentTopologiesData);
    common.home();
    authentication.login('applicationManager');
  });

  it('should be able to define properties as input or output and see their values in application details view', function() {
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.togglePropertyInput('Compute', 'architecture', 'cap');
    topologyEditorCommon.togglePropertyOutput('Compute', 'distribution', 'cap');
    topologyEditorCommon.togglePropertyInput('Java', 'component_version');
    topologyEditorCommon.togglePropertyOutput('Java', 'java_home');
    topologyEditorCommon.toggleAttributeOutput('Compute', 'ip_address');
    checkPropertyState();

    // check again after reloading the page
    applications.goToApplicationTopologyPage();
    checkPropertyState();

    common.go('applications', 'deployment');
    common.go('applicationDeployment', 'input');
    var inputTable = browser.element(by.id('inputsTable'));
    expect(inputTable.element(by.id('p_name_architecture')).isPresent()).toBe(true);
    expect(inputTable.element(by.id('p_name_component_version')).isPresent()).toBe(true);

    common.go('applications', 'deployment');
    common.go('applicationDeployment', 'deploy');
    var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
    browser.actions().click(deployButton).perform();
    common.element(by.binding('APPLICATIONS.UNDEPLOY'), null, 15000);
    common.go('applications', 'info');
    var outputTableText = browser.element(by.id('outputPropertiesTable')).getText();
    expect(outputTableText).toContain('distribution');
    expect(outputTableText).toContain('ubuntu');
    expect(outputTableText).toContain('ip_address');
    expect(outputTableText).toContain('10.52.0.');

    common.go('applications', 'deployment');
    common.go('applicationDeployment', 'deploy');
    var undeployButton = browser.element(by.binding('APPLICATIONS.UNDEPLOY'));
    browser.actions().click(undeployButton).perform();
    common.element(by.binding('APPLICATIONS.DEPLOY'), null, 15000);
    common.go('applications', 'info');
    outputTableText = browser.element(by.id('outputPropertiesTable')).getText();
    expect(outputTableText).not.toContain('ip_address');
    expect(outputTableText).not.toContain('10.52.0.');
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
