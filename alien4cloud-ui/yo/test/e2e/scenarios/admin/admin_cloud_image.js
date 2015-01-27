/* global by*/
'use strict';
var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var cloudImagesCommon = require('../../admin/cloud_image');
var genericForm = require('../../generic_form/generic_form');
var cloudsCommon = require('../../admin/clouds_common');

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
    cloudImagesCommon.addNewCloudImage('test-delete', 'windows', 'x86_32', 'Seven', '7.1', '4', '345', '1024');
    cloudImagesCommon.goToCloudImageList();
    expect(cloudImagesCommon.countCloudImages()).toBe(2);
    common.deleteWithConfirm('btn-delete-cloud-imagetest-delete', true);
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
  });

  it('should be able to rename a cloud image.', function() {
    console.log('################# should be able to rename a cloud image.');
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
    element.all(by.repeater('cloudImage in data.data')).first().click();
    element(by.id('btn-edit-cloud-image')).click();
    genericForm.sendValueToPrimitive('name', 'test-rename', false, 'xeditable');
    cloudImagesCommon.goToCloudImageList();
    expect(element(by.id('cloudImageName_test-rename')).getText()).toEqual('test-rename');
  });

  it('should reject a new image if an image with the same name already exist.', function() {
    console.log('################# should reject a new image if an image with the same name already exist.');
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
    cloudImagesCommon.addNewCloudImage('test-rename', 'linux', 'x86_64', 'Ubuntu', '14.04', '8', '320', '4096');
    genericForm.cancelForm();
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
  });

  it('should be able to edit details of a cloud image.', function() {
    console.log('################# should be able to edit details of a cloud image.');
    expect(cloudImagesCommon.countCloudImages()).toBe(1);
    element.all(by.repeater('cloudImage in data.data')).first().click();
    element(by.id('btn-edit-cloud-image')).click();
    genericForm.sendValueToPrimitive('osType', 'windows', false, 'select');
    genericForm.sendValueToPrimitive('osArch', 'x86_32', false, 'select');
    genericForm.sendValueToPrimitive('osDistribution', 'XP', false, 'xeditable');
    genericForm.sendValueToPrimitive('osVersion', '14.04', false, 'xeditable');
    genericForm.sendValueToPrimitive('numCPUs', '4', false, 'xeditable');
    genericForm.sendValueToPrimitive('diskSize', '640', false, 'xeditable');
    genericForm.sendValueToPrimitive('memSize', '1024', false, 'xeditable');
    cloudImagesCommon.goToCloudImageList();
    element.all(by.repeater('cloudImage in data.data')).first().click();
    genericForm.expectValueFromPrimitive('osType', 'windows', 'xeditable');
    genericForm.expectValueFromPrimitive('osArch', 'x86_32', 'xeditable');
    genericForm.expectValueFromPrimitive('osDistribution', 'XP', 'xeditable');
    genericForm.expectValueFromPrimitive('osVersion', '14.04', 'xeditable');
    genericForm.expectValueFromPrimitive('numCPUs', '4', 'xeditable');
    genericForm.expectValueFromPrimitive('diskSize', '640', 'xeditable');
    genericForm.expectValueFromPrimitive('memSize', '1024', 'xeditable');
  });

  it('should be able to edit few details of a cloud image linked to more than 1 clouds', function() {
    after = true;
    console.log('################# should be able to edit few details of a cloud image linked to more than 1 clouds.');
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('cloud1');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('cloud1');
    cloudsCommon.selectFirstImageOfCloud();
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('cloud2');
    cloudsCommon.goToCloudList();
    cloudsCommon.goToCloudDetail('cloud2');
    cloudsCommon.selectFirstImageOfCloud();
    cloudsCommon.goToCloudList();

    cloudImagesCommon.goToCloudImageList();
    element.all(by.repeater('cloudImage in data.data')).first().click();
    element(by.id('btn-edit-cloud-image')).click();
    // these fields are not editable
    genericForm.expectValueFromPrimitive('osType', 'windows', 'xeditable');
    genericForm.expectValueFromPrimitive('osArch', 'x86_32', 'xeditable');
    genericForm.expectValueFromPrimitive('osDistribution', 'XP', 'xeditable');
    genericForm.expectValueFromPrimitive('osVersion', '14.04', 'xeditable');
    // these fields are editable
    genericForm.sendValueToPrimitive('numCPUs', '16', false, 'xeditable');
    genericForm.sendValueToPrimitive('diskSize', '720', false, 'xeditable');
    genericForm.sendValueToPrimitive('memSize', '4096', false, 'xeditable');
    cloudImagesCommon.goToCloudImageList();
    element.all(by.repeater('cloudImage in data.data')).first().click();
    genericForm.expectValueFromPrimitive('osType', 'windows', 'xeditable');
    genericForm.expectValueFromPrimitive('osArch', 'x86_32', 'xeditable');
    genericForm.expectValueFromPrimitive('osDistribution', 'XP', 'xeditable');
    genericForm.expectValueFromPrimitive('osVersion', '14.04', 'xeditable');
    genericForm.expectValueFromPrimitive('numCPUs', '16', 'xeditable');
    genericForm.expectValueFromPrimitive('diskSize', '720', 'xeditable');
    genericForm.expectValueFromPrimitive('memSize', '4096', 'xeditable');
  });

});
