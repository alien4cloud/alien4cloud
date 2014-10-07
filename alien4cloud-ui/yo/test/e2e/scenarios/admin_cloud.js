/* global by */
'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var cloudsCommon = require('../admin/clouds_common');

describe('Disabling / Enabling cloud: ', function() {
  beforeEach(function() {
    common.before();
    authentication.login('admin');
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
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
});
