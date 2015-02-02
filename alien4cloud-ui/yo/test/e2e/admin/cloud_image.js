/* global element, by */
'use strict';
var common = require('../common/common');
var navigation = require('../common/navigation');
var genericForm = require('../generic_form/generic_form');


var goToCloudImageList = function() {
  navigation.go('main', 'admin');
  navigation.go('admin', 'cloud-images');
};
module.exports.goToCloudImageList = goToCloudImageList;

var fillCloudImageCreationForm = function(name, osType, osArch, osDistribution, osVersion, numCPUs, diskSize, memSize, formId) {
  genericForm.sendValueToPrimitive('name', name, false, 'input');
  genericForm.sendValueToPrimitive('osType', osType, false, 'select');
  genericForm.sendValueToPrimitive('osArch', osArch, false, 'select');
  genericForm.sendValueToPrimitive('osDistribution', osDistribution, false, 'input');
  genericForm.sendValueToPrimitive('osVersion', osVersion, false, 'input');
  genericForm.sendValueToPrimitive('numCPUs', numCPUs, false, 'input');
  genericForm.sendValueToPrimitive('diskSize', diskSize, false, 'input');
  genericForm.sendValueToPrimitive('memSize', memSize, false, 'input');
  if (formId === null || formId === undefined) {
    browser.actions().click(element(by.binding('GENERIC_FORM.SAVE'))).perform();
  } else {
    browser.actions().click(element(by.id(formId)).element(by.binding('GENERIC_FORM.SAVE'))).perform();
  }
  browser.waitForAngular();
  common.dismissAlertIfPresent();
}
module.exports.fillCloudImageCreationForm = fillCloudImageCreationForm;

var addNewCloudImage = function(name, osType, osArch, osDistribution, osVersion, numCPUs, diskSize, memSize) {
  goToCloudImageList();
  element(by.id('new-cloud-image-button')).click();
  browser.waitForAngular();
  fillCloudImageCreationForm(name, osType, osArch, osDistribution, osVersion, numCPUs, diskSize, memSize, null);
};
module.exports.addNewCloudImage = addNewCloudImage;

var countCloudImages = function() {
  goToCloudImageList();
  browser.waitForAngular();
  return element.all(by.repeater('cloudImage in data.data')).count();
};
module.exports.countCloudImages = countCloudImages;
