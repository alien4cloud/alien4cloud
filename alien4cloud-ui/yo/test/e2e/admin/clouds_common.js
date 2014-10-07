/* global by, element, UTILS */
'use strict';

// var SCREENSHOT = require('./screenshot');
var authentication = require('../authentication/authentication');
var common = require('../common/common');
var rolesCommon = require('../common/roles_common');
var navigation = require('../common/navigation');

// Create and enable a default cloud
var beforeWithCloud = function() {

  common.before();
  authentication.login('admin');
  goToCloudList();
  createNewCloud('testcloud');
  goToCloudDetail('testcloud');
  enableCloud();
  authentication.logout();

};
module.exports.beforeWithCloud = beforeWithCloud;

var goToCloudList = function() {
  navigation.go('main', 'admin');
  navigation.go('admin', 'clouds');
};
module.exports.goToCloudList = goToCloudList;

var createNewCloud = function(newCloudName) {
  browser.element(by.id('new-cloud-button')).click();
  browser.waitForAngular();

  element(by.id('cloud-name-id')).sendKeys(newCloudName);
  var paaSProviderOption = element(by.css('select option:nth-child(2)'));
  paaSProviderOption.click();

  var btnCreate = browser.element(by.id('new-cloud-create-button'));
  // SCREENSHOT.takeScreenShot('cloud-common-create-' + newCloudName);
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();
};
module.exports.createNewCloud = createNewCloud;

var goToCloudDetail = function(cloudName) {
  browser.element(by.name(cloudName)).click();
  browser.waitForAngular();
};
module.exports.goToCloudDetail = goToCloudDetail;

var enableCloud = function() {
  browser.element(by.id('cloud-enable-button')).click();
  browser.waitForAngular();
};
module.exports.enableCloud = enableCloud;

var disableCloud = function() {
  browser.element(by.id('cloud-disable-button')).click();
  browser.waitForAngular();
};
module.exports.disableCloud = disableCloud;

// select the cloud plugin by name
var selectApplicationCloud = function(pluginName) {
  var selectElement = element(by.id('cloud-select'));
  var selectResult = common.selectDropdownByText(selectElement, pluginName, 1);
  return selectResult; // promise
};
module.exports.selectApplicationCloud = selectApplicationCloud;

var selectDeploymentPluginCount = function() {
  var count = common.selectCount('cloud-select');
  return count; // promise
};
module.exports.selectDeploymentPluginCount = selectDeploymentPluginCount;

var deleteCloud = function() {
  common.deleteWithConfirm('btn-cloud-delete', true);
  browser.waitForAngular();
  browser.getCurrentUrl().then(function(url) {
    expect(common.getUrlElement(url, 1)).toEqual('admin');
    expect(common.getUrlElement(url, 2)).toEqual('clouds');
  });
};
module.exports.deleteCloud = deleteCloud;

var checkCloudError = function(present) {
  if (present === true) {
    common.expectErrors();
    common.dismissAlert();
  } else {
    common.expectNoErrors();
  }
};
module.exports.checkCloudError = checkCloudError;

var checkDeploymentsDisplayed = function(applicationsNames, displayed) {
  var deploymentsDiv = element(by.id('deployments-div'));
  if (applicationsNames) {
    if (Array.isArray(applicationsNames)) {
      applicationsNames.forEach(function(appName) {
        expect(deploymentsDiv.element(by.name('app_' + appName)).isDisplayed()).toBe(displayed);
      });
    } else {
      expect(deploymentsDiv.element(by.name('app_' + applicationsNames)).isDisplayed()).toBe(displayed);
    }
  }
};
module.exports.checkDeploymentsDisplayed = checkDeploymentsDisplayed;

function selectRightsTabCloud(cloudName, userType, userName, cloudRole) {
  // log as admin
  authentication.reLogin('admin');
  goToCloudList();
  goToCloudDetail(cloudName);
  // go to authorization tab
  element(by.id('rights-tab')).element(by.tagName('a')).click();
  browser.waitForAngular();

  // choose the good tab : users or groups
  element(by.id(userType)).element(by.tagName('a')).click();
  browser.waitForAngular();
  if (userType === 'users-tab') {
    rolesCommon.editUserRole(userName, cloudRole);
  } else { // groups-tab
    rolesCommon.editGroupRole(userName, cloudRole);
  }
}

// Toggle (grant/remove) rights on a cloud for a user (log as admin first)
var giveRightsOnCloudToUser = function(cloudName, user, cloudRole) {
  selectRightsTabCloud(cloudName, 'users-tab', user, cloudRole);
};
module.exports.giveRightsOnCloudToUser = giveRightsOnCloudToUser;

// Toggle (grant/remove) rights on a cloud for a group (log as admin first)
var giveRightsOnCloudToGroup = function(cloudName, group, cloudRole) {
  selectRightsTabCloud(cloudName, 'groups-tab', group, cloudRole);
};
module.exports.giveRightsOnCloudToGroup = giveRightsOnCloudToGroup;
