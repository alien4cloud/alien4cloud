/* global by, element, UTILS */
'use strict';

// var SCREENSHOT = require('./screenshot');
var authentication = require('../authentication/authentication');
var common = require('../common/common');
var rolesCommon = require('../common/roles_common');
var navigation = require('../common/navigation');
var genericForm = require('../generic_form/generic_form');


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
  goToCloudList();
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

var goToCloudDetailImage = function() {
  element(by.id('tab-clouds-image')).element(by.tagName('a')).click();
};
module.exports.goToCloudDetailImage = goToCloudDetailImage;

var goToCloudDetailFlavor = function() {
  element(by.id('tab-clouds-flavor')).element(by.tagName('a')).click();
};
module.exports.goToCloudDetailFlavor = goToCloudDetailFlavor;

var goToCloudDetailTemplate = function() {
  element(by.id('tab-clouds-template')).element(by.tagName('a')).click();
};
module.exports.goToCloudDetailTemplate = goToCloudDetailTemplate;

var countCloud = function() {
  return element.all(by.repeater('cloud in data.data')).count();
};
module.exports.countCloud = countCloud;

var countImageCloud = function() {
  return element.all(by.repeater('cloudImageId in cloud.images')).count();
};
module.exports.countImageCloud = countImageCloud;

var countFlavorCloud = function() {
  return element.all(by.repeater('flavor in cloud.flavors')).count();
};
module.exports.countFlavorCloud = countFlavorCloud;

var countTemplateCloud = function() {
  return element.all(by.repeater('template in cloud.computeTemplates')).count();
};
module.exports.countTemplateCloud = countTemplateCloud;

var addNewFlavor = function(name, numCPUs, diskSize, memSize) {
  browser.element(by.id('clouds-flavor-add-button')).click();
  genericForm.sendValueToPrimitive('id', name, false, 'input');
  genericForm.sendValueToPrimitive('numCPUs', numCPUs, false, 'input');
  genericForm.sendValueToPrimitive('diskSize', diskSize, false, 'input');
  genericForm.sendValueToPrimitive('memSize', memSize, false, 'input');
  element(by.id("new-flavor-generic-form-id")).element(by.binding('GENERIC_FORM.SAVE')).click();
  common.dismissAlertIfPresent();
};
module.exports.addNewFlavor = addNewFlavor;


var selectFirstImageOfCloud = function(nameCloud) {
  goToCloudDetail(nameCloud);
  goToCloudDetailImage();
  browser.element(by.id('clouds-image-add-button')).click();
  var imageLi =  element.all(by.repeater('cloudImage in data.data')).first();
  var imageDiv = imageLi.element(by.css('div[ng-click^="selectImage"]'));
  browser.actions().click(imageDiv).perform();
  browser.element(by.id('clouds-new-image-add-button')).click();
  browser.waitForAngular();
}
module.exports.selectFirstImageOfCloud = selectFirstImageOfCloud;
