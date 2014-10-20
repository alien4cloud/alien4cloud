/* global by*/
'use strict';
var common = require('../common/common');
var authentication = require('../authentication/authentication');
var cloudImagesCommon = require('../admin/cloud_image');
var genericForm = require('../generic_form/generic_form');


describe('List and creation of cloud image', function() {
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

  it('should be able to create a cloud image.', function() {
    console.log('################# should be able to create a cloud image.');
    expect(cloudImagesCommon.countCloudImages()).toBe(0);
    cloudImagesCommon.addNewCloudImage('test-add', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    cloudImagesCommon.goToCloudImageList();
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
  });

  it('should be able to delete a cloud image.', function() {
    console.log('################# should be able to delete a cloud image.');
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
    cloudImagesCommon.addNewCloudImage('test-delete', 'windows', 'x86_32', 'Seven', '7.1', '4', '', '1024');
    cloudImagesCommon.goToCloudImageList();
    expect(cloudImagesCommon.countCloudImages()).toBe(2);
    common.deleteWithConfirm('btn-delete-cloud-imagetest-delete', true);
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
  });

  it('should be able to rename a cloud image.', function() {
    console.log('################# should be able to rename a cloud image.');
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
    element.all(by.repeater('cloudImage in data.data')).first().click();
    common.sendValueToXEditable('template_test-add_name', 'test-rename', false);
    cloudImagesCommon.goToCloudImageList();
    expect(element(by.id('cloudImageName_test-rename')).getText()).toEqual('test-rename');
  });

  it('should reject a new image if an image with the same name already exist.', function() {
    after = true;
    console.log('################# should reject a new image if an image with the same name already exist.');
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
    cloudImagesCommon.addNewCloudImage('test-rename', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    genericForm.cancelForm();
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
  });
});
