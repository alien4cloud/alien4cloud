/* global by, element */
'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var applications = require('../applications/applications');
var cloudImagesCommon = require('../admin/cloud_image');
var cloudsCommon = require('../admin/clouds_common');
var componentData = require('../topology/component_data');
var topologyEditorCommon = require('../topology/topology_editor_common');



describe('Application Deployment :', function() {
  var reset = true;
  var after = false;

  /* Before each spec in the tests suite */
  beforeEach(function() {
    if(reset) {
      reset = false;
      common.before();
      authentication.login('admin');
      cloudsCommon.goToCloudList();
      cloudsCommon.createNewCloud('testcloud');
    }
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    if(after) {
      common.after();
    }
  });

  it('should disable the matcher when the todo-list is not empty.', function() {
    console.log('################# should disable the matcher when the todo-list is not empty.');
    applications.createApplication('Alien', 'Great Application');
    cloudsCommon.goToCloudList();
    cloudImagesCommon.addNewCloudImage('Compute-dev', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    cloudsCommon.selectFirstImageOfCloud('testcloud');
    cloudsCommon.goToCloudDetailFlavor();
    cloudsCommon.addNewFlavor("large", "8", "320", "4096");
    cloudsCommon.goToCloudDetailFlavor();
    cloudsCommon.addNewFlavor("medium", "12", "480", "4096");
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();

    applications.goToApplicationDetailPage('Alien', true);
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.verySimpleTopology.nodes);
    applications.goToApplicationDeploymentPage();
    expect(element(by.id("div-deployment-matcher")).isPresent()).toBe(false);
  });

  it('should be display the matcher when the todo-list is empty.', function() {
    console.log('################# should be display the matcher when the todo-list is empty.');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'linux');
    applications.goToApplicationDeploymentPage();
    cloudsCommon.selectApplicationCloud('testcloud');
    expect(element(by.id("div-deployment-matcher")).isPresent()).toBe(true);
  });

  it('should propose match between the node and the templates.', function() {
    after = true;
    console.log('################# should propose match between the node and the templates.');
    applications.goToApplicationTopologyPage();
    applications.goToApplicationDeploymentPage();
    element(by.id("tr-node-Compute")).click();
    var templates = element.all(by.repeater('template in currentMatchedComputeTemplates'));
    expect(templates.count()).toBe(2);
  });
});
