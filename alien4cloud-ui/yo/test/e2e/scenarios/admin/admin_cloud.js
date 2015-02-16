/* global by */
'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var cloudsCommon = require('../../admin/clouds_common');
var cloudImageCommon = require('../../admin/cloud_image');
var genericForm = require('../../generic_form/generic_form');

describe('Test the cloud management: ', function() {
  var reset = true;
  var after = false;

  beforeEach(function() {
    if (reset) {
      reset = false;
      common.before();
      authentication.login('admin');
    }
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    if (after) {
      common.after();
    }
  });

  it('should be rename a cloud.', function() {
    console.log('################# should be rename a cloud.');
    expect(browser.isElementPresent(by.name('testcloud'))).toBe(false);
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('test-rename');
    cloudsCommon.goToCloudDetail('test-rename');
    cloudsCommon.enableCloud();
    common.sendValueToXEditable('cloud_name_input', 'testcloud', false);
    cloudsCommon.goToCloudList();
    expect(browser.isElementPresent(by.name('testcloud'))).toBe(true);
  });

  it('should create network', function() {
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.addNewNetwork('private', '192.168.0.0/24', false, '192.168.0.1', '4');
    expect(cloudsCommon.countNetworkCloud()).toBe(1);
    cloudsCommon.assignPaaSIdToNetwork('private', 'alienPrivateNetwork');
  });

  it('should reject a new cloud if a cloud with the same name already exist.', function() {
    console.log('################# should reject a new cloud if a cloud with the same name already exist.');
    cloudsCommon.goToCloudList();
    expect(cloudsCommon.countCloud()).toBe(1);
    cloudsCommon.createNewCloud('testcloud');
    expect(cloudsCommon.countCloud()).toBe(1);
    common.dismissAlert();
  });

  it('should be select an image for a cloud.', function() {
    console.log('################# should be select an image for a cloud.');
    cloudImageCommon.addNewCloudImage('test-add', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    cloudsCommon.goToCloudDetail('testcloud');
    expect(cloudsCommon.countImageCloud()).toBe(0);
    cloudsCommon.selectFirstImageOfCloud();
    expect(cloudsCommon.countImageCloud()).toBe(1);
  });

  it('should be create a flavor for a cloud.', function() {
    console.log('################# should be create a flavor for a cloud.')
    expect(cloudsCommon.countFlavorCloud()).toBe(0);
    cloudsCommon.addNewFlavor("small", "1", "10", "256");
    expect(cloudsCommon.countFlavorCloud()).toBe(1);
  });

  it('should create a flavor, assign a PaaS to image and flavor, and have a template matching.', function() {
    console.log('################# should be create a flavor who matching with an image for a cloud.')
    expect(cloudsCommon.countTemplateCloud()).toBe(0);
    cloudsCommon.addNewFlavor("medium", "12", "480", "4096");
    cloudsCommon.goToCloudDetailTemplate('testcloud');
    cloudsCommon.assignPaaSResourceToImage("test-add", "passIdImage1");
    cloudsCommon.assignPaaSResourceToFlavor("medium", "passIdFlavor1");
    expect(cloudsCommon.countTemplateCloud()).toBe(1);
  });

  it('should be able to disable / delete a cloud when nothing is deployed.', function() {
    console.log('################# should be able to disable / delete a cloud when nothing is deployed.');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.disableCloud();
    cloudsCommon.checkCloudError(false);
    cloudsCommon.enableCloud();
    cloudsCommon.deleteCloud();
    expect(browser.isElementPresent(by.name('testcloud'))).toBe(false);
  });
  
  it('should be able to select PaaS resources IDs from a list for PaaS that provide them.', function() {
    after = true;
    console.log('################# should be able to select PaaS resources IDs from a list for PaaS that provide them.');
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('test-id-provided');
    cloudsCommon.goToCloudDetail('test-id-provided');
    cloudsCommon.goToCloudConfiguration();
    // by switching this to true, we make the mock PaaS provider able to send list of PaaS resource IDs
    var badConfigurationSwitch = browser.element(by.id('primitiveTypeFormLabelprovideResourceIdstrue'));
    browser.actions().click(badConfigurationSwitch).perform();
    genericForm.saveForm();
    cloudsCommon.goToCloudDetail('test-id-provided');
    cloudsCommon.enableCloud();    
    // now associate / create resources and associate them to provided resource IDs
    cloudsCommon.addNewCloudImage('MyImage1', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    // expect to have 1 image for this cloud
    expect(cloudsCommon.countImageCloud()).toBe(1);
    cloudsCommon.addNewCloudImage('MyImage2', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    // now expect to have 2 image for this cloud
    expect(cloudsCommon.countImageCloud()).toBe(2);
    // we know the mock returns 10 IDs
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('MyImage1_resourceId', 'yetAnotherResourceId-IMAGE-0', 'value in availaiblePaaSImageIds', 10);
    // now that 1 have been selected for 'MyImage1' we should have 9 remaining available
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('MyImage2_resourceId', 'yetAnotherResourceId-IMAGE-1', 'value in availaiblePaaSImageIds', 9);
    cloudsCommon.addNewFlavor("MEDIUM", "12", "480", "4096");
    expect(cloudsCommon.countFlavorCloud()).toBe(1);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('MEDIUM_resourceId', 'yetAnotherResourceId-FLAVOR-0', 'value in availaiblePaaSFlavorIds', 10);
    // at this stage we should have 2 templates
    expect(cloudsCommon.countTemplateCloud()).toBe(2);

    cloudsCommon.goToCloudDetailImage();
    cloudsCommon.deleteCoudImage('MyImage1');
    expect(cloudsCommon.countImageCloud()).toBe(1);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('MyImage2_resourceId', 'yetAnotherResourceId-IMAGE-0', 'value in availaiblePaaSImageIds', 9);

    cloudsCommon.addNewNetwork('NETWORK1', '', false, '', '4');
    expect(cloudsCommon.countNetworkCloud()).toBe(1);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('NETWORK1_resourceId', 'yetAnotherResourceId-NETWORK-0', 'value in availaiblePaaSNetworkIds', 10);
    cloudsCommon.addNewNetwork('NETWORK2', '', false, '', '4');
    expect(cloudsCommon.countNetworkCloud()).toBe(2);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('NETWORK2_resourceId', 'yetAnotherResourceId-NETWORK-1', 'value in availaiblePaaSNetworkIds', 9);
    cloudsCommon.deleteCoudNetwork('NETWORK1');
    expect(cloudsCommon.countNetworkCloud()).toBe(1);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('NETWORK2_resourceId', 'yetAnotherResourceId-NETWORK-0', 'value in availaiblePaaSNetworkIds', 9);

    cloudsCommon.addNewStorage('STORAGE1', '/etc/dev1', 1024);
    expect(cloudsCommon.countStorageCloud()).toBe(1);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('STORAGE1_resourceId', 'yetAnotherResourceId-VOLUME-0', 'value in availaiblePaaSStorageIds', 10);
    cloudsCommon.addNewStorage('STORAGE2', '/etc/dev2', 1024);
    expect(cloudsCommon.countStorageCloud()).toBe(2);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('STORAGE2_resourceId', 'yetAnotherResourceId-VOLUME-1', 'value in availaiblePaaSStorageIds', 9);
    cloudsCommon.deleteCoudStorage('STORAGE1');
    expect(cloudsCommon.countStorageCloud()).toBe(1);
    cloudsCommon.countAndSelectResourcePaaSIdFromDropDown('STORAGE2_resourceId', 'yetAnotherResourceId-VOLUME-0', 'value in availaiblePaaSStorageIds', 9);
  });

});
