/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');
var cloudImagesCommon = require('../../admin/cloud_image');
var cloudsCommon = require('../../admin/clouds_common');
var componentData = require('../../topology/component_data');
var topologyEditorCommon = require('../../topology/topology_editor_common');


describe('Application Deployment matcher :', function() {
  var reset = true;
  var after = false;

  /* Before each spec in the tests suite */
  beforeEach(function() {
    if (reset) {
      reset = false;
      common.before();
      authentication.login('admin');
      applications.createApplication('Alien', 'Great Application');
      cloudImagesCommon.addNewCloudImage('Compute-dev', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
      cloudsCommon.goToCloudList();
      cloudsCommon.createNewCloud('testcloud');
      cloudsCommon.goToCloudDetail('testcloud');
      // we need to enable cloud to be able to assign PaaS resource IDs
      cloudsCommon.enableCloud();
      cloudsCommon.selectFirstImageOfCloud();
      cloudsCommon.assignPaaSResourceToImage('Compute-dev', 'passIdImage1');
      cloudsCommon.addNewFlavor('large', '8', '320', '4096');
      cloudsCommon.addNewFlavor('medium', '12', '480', '4096');
      cloudsCommon.goToCloudDetailTemplate();
      cloudsCommon.assignPaaSResourceToFlavor('large', 'passIdFlavor1');
      cloudsCommon.assignPaaSResourceToFlavor('medium', 'passIdFlavor2');
      cloudsCommon.goToCloudDetail('testcloud');
      cloudsCommon.disableCloud();
      cloudsCommon.enableCloud();
    }
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    if (after) {
      common.after();
    }
  });

  it('should disable the matcher when the todo-list is not empty.', function() {
    console.log('################# should disable the matcher when the todo-list is not empty.');
    applications.goToApplicationDetailPage('Alien', true);
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.verySimpleTopology.nodes);
    applications.goToApplicationDeploymentPage();
    expect(element(by.id('div-deployment-matcher')).isPresent()).toBe(false);
  });

  it('should be display the matcher when the todo-list is empty.', function() {
    console.log('################# should be display the matcher when the todo-list is empty.');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'linux');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    applications.goToApplicationDeploymentPage();
    cloudsCommon.selectApplicationCloud('testcloud');
    expect(element(by.id('div-deployment-matcher')).isPresent()).toBe(true);
  });

  it('should propose match between the node and the templates.', function() {
    console.log('################# should propose match between the node and the templates.');
    element(by.id('tr-node-Compute')).click();
    var templates = element.all(by.repeater('template in currentMatchedComputeTemplates'));
    expect(templates.count()).toBe(2);
  });

  it('should display the matcher when we add a network without network in cloud.', function() {
    console.log('################# should display the matcher when we add a network without network in cloud.');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({ network: componentData.toscaBaseTypes.network() });
    applications.goToApplicationDeploymentPage();
    topologyEditorCommon.expectDeploymentWork(false, false);
  });

  it('should be able to asociate a network.', function() {
    console.log('################# should be able to asociate a network.');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewNetwork('NETWORK1', '', false, '', '4');
    expect(cloudsCommon.countNetworkCloud()).toBe(1);

    applications.goToApplicationDetailPage('Alien', true);
    applications.goToApplicationDeploymentPage();
    element(by.id('tab-network-resources')).element(by.tagName('a')).click();
    browser.element(by.id('tr-node-Network')).click();
    browser.actions().click(element.all(by.repeater('network in currentMatchedNetworks')).first()).perform();
    browser.waitForAngular();
    topologyEditorCommon.expectDeploymentWork(false, true);
  });

  it('should display the matcher when we add a storage without storage in cloud.', function() {
    console.log('################# should display the matcher when we add a volume without volume in cloud.');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({ network: componentData.toscaBaseTypes.blockstorage() });
    topologyEditorCommon.addRelationshipToNode('BlockStorage', 'Compute', 'attach', 'tosca.relationships.HostedOn:2.0', 'hostedOnCompute');
    applications.goToApplicationDeploymentPage();
    browser.waitForAngular();
    topologyEditorCommon.expectDeploymentWork(false, false);
  });

  it('should be able to asociate a storage.', function() {
    after = true;
    console.log('################# should be able to asociate a storage.');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewStorage('STORAGE1', '/etc/dev1', 1024);
    expect(cloudsCommon.countStorageCloud()).toBe(1);

    applications.goToApplicationDetailPage('Alien', true);
    applications.goToApplicationDeploymentPage();
    element(by.id('tab-storage-resources')).element(by.tagName('a')).click();
    browser.element(by.id('tr-node-BlockStorage')).click();
    browser.actions().click(element.all(by.repeater('(key, templates) in matchedStorageResources')).first()).perform();
    browser.waitForAngular();
    topologyEditorCommon.expectDeploymentWork(false, true);
  });
});
