'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var navigation = require('../../common/navigation');
var applications = require('../../applications/applications');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');
var cloudsCommon = require('../../admin/clouds_common');
var rolesCommon = require('../../common/roles_common');
var cloudImageCommon = require('../../admin/cloud_image');

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
    browser.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.addRelationshipToNode('Compute', 'Network', 'network', 'tosca.relationships.Network:2.0', 'connectedToNetwork');
    });
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
  });

  afterEach(function() {
    common.after();
  });



  it('should not be able to deploy application if resource is not matched', function() {
    console.log('should not be able to deploy application if compute is not matched');
    cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
    applications.goToApplicationDetailPage('Alien', false);
    navigation.go('applications', 'deployment');
    var selected = cloudsCommon.selectApplicationCloud('testcloud');
    expect(selected).toBe(true);
    // Deployment do not work as no compute template added to cloud
    topologyEditorCommon.expectDeploymentWork(false, false);

    // Fill cloud with proper resources matching
    authentication.reLogin('admin');
    cloudImageCommon.addNewCloudImage('Windows', 'windows', 'x86_64', 'Windows', '14.04', '1', '1', '1')
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewFlavor('medium', '12', '480', '4096');
    cloudsCommon.selectFirstImageOfCloud();
    cloudsCommon.assignPaaSResourceToImage("Windows", "passIdImage1");
    cloudsCommon.assignPaaSResourceToFlavor("medium", "passIdFlavor1");
    cloudsCommon.goToCloudDetail('testcloud');

    // The deploy button must not be available
    topologyEditorCommon.expectDeploymentWork(true, false);

    authentication.reLogin('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewNetwork('private', '192.168.0.0/24', false, '192.168.0.1', '4');
    cloudsCommon.assignPaaSIdToNetwork('private', 'alienPrivateNetwork');

    // The deploy button must be available
    topologyEditorCommon.expectDeploymentWork(true, true);
  });

});
