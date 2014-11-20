/* global element, by */
'use strict';

var path = require('path');
var authentication = require('../authentication/authentication');
var common = require('../common/common');
var cleanup = require('../common/cleanup');
var pluginsCommon = require('../admin/plugins_common');
var navigation = require('../common/navigation');
var SCREENSHOT = require('../common/screenshot');

// Exposing upload csar functionality
var pathToBaseTypes = path.resolve(__dirname, '../../../../../alien4cloud-core/src/test/resources/examples/tosca-base-types-1.0.csar');
var pathToJavaTypes = path.resolve(__dirname, '../../../../../alien4cloud-core/src/test/resources/examples/java-types-1.0.csar');
var pathToBaseTypesV2 = path.resolve(__dirname, '../../../../../alien4cloud-core/src/test/resources/examples/tosca-base-types-2.0.csar');
var pathToJavaTypesV2 = path.resolve(__dirname, '../../../../../alien4cloud-core/src/test/resources/examples/java-types-2.0.csar');
var pathToApacheLbTypes = path.resolve(__dirname, '../../../../../alien4cloud-core/src/test/resources/examples/apacheLB-types-0.2.csar');
var pathToUbuntuType = path.resolve(__dirname, '../../../../../alien4cloud-core/src/test/resources/examples/ubuntu-types-0.1.csar');


describe('Initialize test environment', function() {
  beforeEach(function() {
    browser.driver.manage().window().setSize(1920, 1080);
    browser.driver.manage().window().maximize();
    navigation.home();
  });

  afterEach(function() {
    common.after();
  });

  it('Setups test environment to be fully cleaned up', function() {
    browser.driver.manage().window().getSize().then(function(size) {
      console.log('################# Window\'s size  [' + size.width + ', ' + size.height + ']');
    });
    console.log('################# Setups test environment to be fully cleaned up');
    cleanup.fullCleanup();
    navigation.home();
    authentication.login('admin');
  });

  it('Admin should be able to upload component archives', function() {
    console.log('################# Admin should be able to upload component archives');
    authentication.login('admin');
    navigation.go('main', 'components');

    common.uploadFile(pathToBaseTypes);
    common.uploadFile(pathToJavaTypes);
    SCREENSHOT.takeScreenShot('upload-components');
    common.removeAllFacetFilters();
    expect(element.all(by.repeater('component in searchResult.data.data')).count()).toEqual(20);
  });

  it('Component manager should be able to upload component archives', function() {
    console.log('################# Component manager should be able to upload component archives');
    authentication.login('componentManager');
    navigation.go('main', 'components');

    common.uploadFile(pathToBaseTypesV2);
    common.uploadFile(pathToJavaTypesV2);
    common.uploadFile(pathToApacheLbTypes);
    common.uploadFile(pathToUbuntuType);
  });

  it('Admin should be able to upload plugin', function() {
    console.log('################# Admin should be able to upload plugin');
    authentication.login('admin');
    // upload mock paas plugin
    pluginsCommon.pluginsUploadInit();
  });
});
