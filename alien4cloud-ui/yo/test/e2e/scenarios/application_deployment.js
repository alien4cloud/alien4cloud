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

  it('should disable the matcher if todo-list is not empty.', function() {
    console.log('################# should disable the matcher if todo-list is not empty.');
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

  it('should asociate a cloud template to a node.', function() {
    after = true;
    console.log('################# should add a good tag without errors.');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'linux');
    applications.goToApplicationDeploymentPage();
    expect(element(by.id("div-deployment-matcher")).isPresent()).toBe(true);
  });
});
