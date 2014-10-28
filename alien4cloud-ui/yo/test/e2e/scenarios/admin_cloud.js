/* global by */
'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var cloudsCommon = require('../admin/clouds_common');
var cloudImageCommon = require('../admin/cloud_image');


describe('Test the cloud management: ', function() {
  var reset = true;
  var after = false;

  beforeEach(function() {
    if(reset) {
      reset = false;
      common.before();
      authentication.login('admin');
    }
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    if(after) {
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
    cloudsCommon.goToCloudDetail('testcloud');
    expect(cloudsCommon.countImageCloud()).toBe(0);
    cloudImageCommon.addNewCloudImage('test-add', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    cloudsCommon.selectFirstImageOfCloud('testcloud');
    expect(cloudsCommon.countImageCloud()).toBe(1);
  });

  it('should be create a flavor for a cloud.', function() {
    console.log('################# should be create a flavor for a cloud.')
    expect(cloudsCommon.countFlavorCloud()).toBe(0);
    cloudsCommon.goToCloudDetailFlavor();
    cloudsCommon.addNewFlavor("small", "1", "10", "256");
    expect(cloudsCommon.countFlavorCloud()).toBe(1);
  });

  it('should be create a flavor who matching with an image for a cloud.', function() {
    console.log('################# should be create a flavor who matching with an image for a cloud.')
    expect(cloudsCommon.countTemplateCloud()).toBe(0);
    cloudsCommon.addNewFlavor("medium", "12", "480", "4096");
    cloudsCommon.goToCloudDetailTemplate('testcloud');
    expect(cloudsCommon.countTemplateCloud()).toBe(1);
  });

  it('should be able to disable / delete a cloud when nothing is deployed.', function() {
    after = true;
    console.log('################# should be able to disable / delete a cloud when nothing is deployed.');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.disableCloud();
    cloudsCommon.checkCloudError(false);
    cloudsCommon.enableCloud();
    cloudsCommon.deleteCloud();
    expect(browser.isElementPresent(by.name('testcloud'))).toBe(false);
  });

});
