/* global by*/
'use strict';
var common = require('../common/common');
var authentication = require('../authentication/authentication');
var cloudImagesCommon = require('../admin/cloud_image');
var genericForm = require('../generic_form/generic_form');


describe('List and creation of cloud image', function() {
  beforeEach(function() {
    common.before();
    authentication.login('admin');
    cloudImagesCommon.goToCloudImageList();
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to create a cloud image.', function() {
    expect(element.all(by.repeater('cloudImage in data.data')).count()).toBe(0);
    cloudImagesCommon.addNewCloudImage('test-add', 'linux', 'x86_64', 'Ubuntu', '14.04');
    expect(element.all(by.repeater('cloudImage in data.data')).count()).toBe(1);
  });

  it('should be able to delete a cloud image.', function() {
    cloudImagesCommon.addNewCloudImage('test-delete', 'windows', 'x86_32', 'Seven', '7.1');
    expect(element.all(by.repeater('cloudImage in data.data')).count()).toBe(2);
    common.deleteWithConfirm('btn-delete-cloud-imagetest-delete', true);
    expect(element.all(by.repeater('cloudImage in data.data')).count()).toBe(1);
  });
});
