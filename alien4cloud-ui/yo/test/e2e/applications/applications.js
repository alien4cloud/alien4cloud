/* global by, element */

'use strict';

var navigation = require('../common/navigation');
var common = require('../common/common');
var SCREENSHOT = require('../common/screenshot');
var topologyEditorCommon = require('../topology/topology_editor_common');
var cloudsCommon = require('../admin/clouds_common');
var rolesCommon = require('../common/roles_common');

module.exports.checkApplicationManager = function(isManager) {
  navigation.go('main', 'applications');
  var message = isManager ? 'A user that is application manager or admin should have access to the create application button on the applications list page.' : 'A user that is not application manager or admin should not have access to the create application button on the applications list page.';
  expect(browser.element(by.binding('APPLICATIONS.NEW')).isPresent()).toBe(isManager, message);
};

function goToApplicationListPage() {
  navigation.go('main', 'applications');
}
module.exports.goToApplicationListPage = goToApplicationListPage;

function goToApplicationDetailPage(applicationName, goOnTopology) {
  navigation.go('main', 'applications');
  // From the application search page select a particular line
  var appElement = element(by.id('app_' + applicationName)); // .click();
  appElement.click();

  if (goOnTopology === true) {
    navigation.go('applications', 'topology');
  }

  browser.waitForAngular();
}
module.exports.goToApplicationDetailPage = goToApplicationDetailPage;

// Expose create application functions
var createApplication = function(newAppName, newAppDescription, topologyTemplateSelectNumber) {
  navigation.go('main', 'applications');
  // Can add a new application
  var btnNewApplication = browser.element(by.binding('APPLICATIONS.NEW'));
  browser.actions().click(btnNewApplication).perform();

  element(by.model('app.name')).sendKeys(newAppName);
  element(by.model('app.description')).sendKeys(newAppDescription);

  // Template to select
  if (typeof topologyTemplateSelectNumber !== 'undefined') {
    // topologyTemplateSelectNumber should start at 2 since the one at 1 is (no template) first ins the list
    var select = element(by.css('select option:nth-child(' + topologyTemplateSelectNumber + ')'));
    select.click();
  }

  // Create an App and find it in the list
  var btnCreate = browser.element(by.binding('CREATE'));
  SCREENSHOT.takeScreenShot('common-createapp-' + newAppName);
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();
};
module.exports.createApplication = createApplication;


module.exports.deploy = function(applicationName, nodeTemplates) {
  cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
  goToApplicationDetailPage(applicationName, true);
  topologyEditorCommon.addNodeTemplatesCenterAndZoom(nodeTemplates);
  if (nodeTemplates.compute) {
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
  }
  goToApplicationDetailPage(applicationName, false);
  navigation.go('applications', 'deployment');
  var selected = cloudsCommon.selectApplicationCloud('testcloud');
  expect(selected).toBe(true); // testcloud is in the select
  navigation.go('applications', 'info');
  navigation.go('applications', 'deployment');
  browser.sleep(1000); // DO NOT REMOVE, wait few seconds for the ui to be ready
  var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
  browser.actions().click(deployButton).perform();
  browser.sleep(5000); // DO NOT REMOVE, button clickable few seconds after DEPLOY click
};

var setDeploymentProperty = function(propertyName, propertyValue) {
  common.sendValueToXEditable('p_' + propertyName, propertyValue);
};

module.exports.deployExistingApplication = function(applicationName) {
  cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
  goToApplicationDetailPage(applicationName, false);
  navigation.go('applications', 'deployment');
  var selected = cloudsCommon.selectApplicationCloud('testcloud');
  expect(selected).toBe(true); // testcloud is in the select
  navigation.go('applications', 'info');
  navigation.go('applications', 'deployment');
  browser.sleep(1000);

  // enter deployment topology properties : mock paas provider
  setDeploymentProperty('managementUrl', 'http://passmanager:8099');
  setDeploymentProperty('managerEmail', 'admin@alien.fr');
  setDeploymentProperty('numberBackup', 1);

  var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
  browser.actions().click(deployButton).perform();
  browser.sleep(2000); // DO NOT REMOVE, button clickable few seconds after DEPLOY click

  navigation.go('applications', 'runtime');
};
