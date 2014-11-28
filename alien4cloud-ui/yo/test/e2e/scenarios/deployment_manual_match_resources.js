'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var navigation = require('../common/navigation');
var applications = require('../applications/applications');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');
var cloudsCommon = require('../admin/clouds_common');
var rolesCommon = require('../common/roles_common');
var cloudImageCommon = require('../admin/cloud_image');

var nodeTemplates = {
  compute: componentData.toscaBaseTypes.compute(),
  network: componentData.toscaBaseTypes.network()
};

describe('Manually match resources for cloud', function() {

  beforeEach(function() {
    common.before();
    authentication.login('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();
    authentication.reLogin('applicationManager');
    applications.createApplication('Alien', 'Great Application');
    // Go to the app details page
    browser.element(by.binding('application.name')).click();
    navigation.go('applications', 'topology');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(nodeTemplates);
    common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.addRelationshipToNode('Compute', 'Network', 'network', 'tosca.relationships.Network:2.0', 'connectedToNetwork');
    });
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
  });

  afterEach(function() {
    common.after();
  });

  var expectDeploymentWork = function(goToAppDetail, work) {
    if (goToAppDetail) {
      authentication.reLogin('applicationManager');
      applications.goToApplicationDetailPage('Alien', false);
      navigation.go('applications', 'deployment');
    }
    var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
    if (work) {
      expect(deployButton.getAttribute('disabled')).toBeNull();
      expect(element(by.id('div-deployment-matcher')).element(by.tagName('legend')).element(by.tagName('i')).getAttribute('class')).not.toContain('text-danger');
    } else {
      expect(deployButton.getAttribute('disabled')).toEqual('true');
      expect(element(by.id('div-deployment-matcher')).element(by.tagName('legend')).element(by.tagName('i')).getAttribute('class')).toContain('text-danger');
    }
  };

  it('should not be able to deploy application if resource is not matched', function() {
    console.log('should not be able to deploy application if compute is not matched');
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    applications.goToApplicationDetailPage('Alien', false);
    navigation.go('applications', 'deployment');
    var selected = cloudsCommon.selectApplicationCloud('testcloud');
    expect(selected).toBe(true);
    // Deployment do not work as no compute template added to cloud
    expectDeploymentWork(false, false);

    // Fill cloud with proper resources matching
    authentication.reLogin('admin');
    cloudImageCommon.addNewCloudImage('Windows', 'windows', 'x86_64', 'Windows', '14.04', '1', '1', '1')
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewFlavor('medium', '12', '480', '4096');
    cloudsCommon.selectFirstImageOfCloud();
    cloudsCommon.assignPaaSResourceToTemplate('Windows', 'medium', 'MEDIUM_WINDOWS');

    // The deploy button must not be available
    expectDeploymentWork(true, false);

    authentication.reLogin('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewNetwork('private', '192.168.0.0/24', '192.168.0.1', '4');
    cloudsCommon.assignPaaSIdToNetwork('private', 'alienPrivateNetwork');

    // The deploy button must be available
    expectDeploymentWork(true, true);
  });

});