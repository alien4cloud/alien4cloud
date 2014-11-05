'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var navigation = require('../common/navigation');
var applications = require('../applications/applications');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');
var cloudsCommon = require('../admin/clouds_common');

var nodeTemplates = {
  java: componentData.fcTypes.javaRPM(),
  compute: componentData.toscaBaseTypes.compute()
};

describe('Manually match resources for cloud', function() {

  beforeEach(function() {
    common.before();
    authentication.login('admin');
    // cloudImageCommon.addNewCloudImage('Windows', 'windows', 'x86_64', 'Windows', '14.04', '1', '1', '1')
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();
    // cloudsCommon.addNewFlavor('medium', '12', '480', '4096');
    // cloudsCommon.selectFirstImageOfCloud();
    // cloudsCommon.assignPaaSResourceToTemplate('Windows', 'medium', 'MEDIUM_WINDOWS');
    authentication.logout();

    authentication.login('applicationManager');
    applications.createApplication('Alien', 'Great Application');
    // Go to the app details page
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'topology');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(nodeTemplates);
  });

  afterEach(function() {
    common.after();
  });

  it('should not be able to deploy application if compute is not matched', function() {
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    goToApplicationDetailPage(applicationName, false);
    navigation.go('applications', 'deployment');
    var selected = cloudsCommon.selectApplicationCloud('testcloud');
    expect(selected).toBe(true); // testcloud is in the select
    navigation.go('applications', 'info');
    navigation.go('applications', 'deployment');
    var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
    expect(deployButton.getAttribute('class')).toContain('disabled');
    expect(element(by.id('div-deployment-matcher').element(by.tagName('legend')).element(by.tagName('i'))).getAttribute('class')).toContain('text-danger');
  });

});