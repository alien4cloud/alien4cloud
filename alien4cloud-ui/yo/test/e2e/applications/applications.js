/* global by, element */

'use strict';

var navigation = require('../common/navigation');
var common = require('../common/common');
var SCREENSHOT = require('../common/screenshot');
var topologyEditorCommon = require('../topology/topology_editor_common');
var cloudsCommon = require('../admin/clouds_common');
var rolesCommon = require('../common/roles_common');

var environmentTypes = {
  other: 'OTHER',
  dev: 'DEVELOPMENT',
  it: 'INTEGRATION_TESTS',
  uat: 'USER_ACCEPTANCE_TESTS',
  pprod: 'PRE_PRODUCTION',
  prod: 'PRODUCTION'
};
module.exports.environmentTypes = environmentTypes;

var mockPaaSDeploymentProperties = {
  // enter deployment topology properties : mock paas provider
  managementUrl: 'http://passmanager:8099',
  managerEmail: 'admin@alien.fr',
  numberBackup: 1
};
module.exports.mockPaaSDeploymentProperties = mockPaaSDeploymentProperties;

var mockDeploymentPropertiesMordor = {
  managementUrl: 'http://mordor:666',
  managerEmail: 'mordor@alien.fr',
  numberBackup: 666
};
module.exports.mockDeploymentPropertiesMordor = mockDeploymentPropertiesMordor;

module.exports.checkApplicationManager = function(isManager) {
  navigation.go('main', 'applications');
  var message = isManager ? 'A user that is application manager or admin should have access to the create application button on the applications list page.' : 'A user that is not application manager or admin should not have access to the create application button on the applications list page.';
  expect(browser.element(by.binding('APPLICATIONS.NEW')).isPresent()).toBe(isManager, message);
};

function goToApplicationListPage() {
  navigation.go('main', 'applications');
}
module.exports.goToApplicationListPage = goToApplicationListPage;

function goToApplicationDeploymentPage() {
  navigation.go('applications', 'deployment');
}
module.exports.goToApplicationDeploymentPage = goToApplicationDeploymentPage;

function goToApplicationTopologyPage() {
  navigation.go('applications', 'topology');
}
module.exports.goToApplicationTopologyPage = goToApplicationTopologyPage;

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

// DEPLOYMENT HANDLING

var setDeploymentProperty = function(propertyName, propertyValue) {
  common.sendValueToXEditable('p_' + propertyName, propertyValue);
};

var setMockPaasProperties = function(propertiesObj) {
  setDeploymentProperty('managementUrl', propertiesObj.managementUrl);
  setDeploymentProperty('managerEmail', propertiesObj.managerEmail);
  setDeploymentProperty('numberBackup', propertiesObj.numberBackup);
};

var switchEnvironmentAndCloud = function(envName, cloudName) {
  // default values for select env / cloud
  cloudName = cloudName === null ? 'testcloud' : cloudName;
  envName = envName === null ? 'Environment' : envName;
  var selectedEnvironment = selectApplicationEnvironment(envName);
  var selectedCloud = cloudsCommon.selectApplicationCloud(cloudName);
  expect(selectedEnvironment).toBe(true); // one cloud selected
  expect(selectedCloud).toBe(true); // one cloud selected
};
module.exports.switchEnvironmentAndCloud = switchEnvironmentAndCloud;

module.exports.setupDeploymentProperties = function(appName, envName, cloudName, propertiesObject) {

  // go on this, deployment sub-menu
  goToApplicationDetailPage(appName, false);
  navigation.go('applications', 'deployment');

  // select an env / cloud
  switchEnvironmentAndCloud(envName, cloudName);
  setMockPaasProperties(propertiesObject);
};

var simpleDeploy = function justADeployClick() {
  var deployButton = browser.element(by.id('btn-deploy'));
  browser.actions().click(deployButton).perform();
};
module.exports.simpleDeploy = simpleDeploy;

module.exports.deploy = function(applicationName, nodeTemplates, cloudName, environmentName, deploymentProperties) {

  // handle cloud / environment to use
  cloudName = cloudName === null ? 'testcloud' : cloudName;
  environmentName = environmentName === null ? 'Environment' : environmentName;
  cloudsCommon.giveRightsOnCloudToUser(cloudName, 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);

  // complete the topology : one compue
  if (nodeTemplates !== null) {
    goToApplicationDetailPage(applicationName, true);
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(nodeTemplates);
    if (nodeTemplates.compute) {
      topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
      topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
      topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    }
  }

  // go on application page to perform the deploy
  goToApplicationDetailPage(applicationName, false);
  navigation.go('applications', 'deployment');
  switchEnvironmentAndCloud(environmentName, cloudName);

  // cloud selected => enter properties when cloud selected
  if (deploymentProperties !== null) {
    // enter deployment properties : mock paas provider
    setMockPaasProperties(deploymentProperties);
  }

  // DEPLOY
  browser.sleep(1000); // DO NOT REMOVE, wait few seconds for the ui to be ready
  simpleDeploy();
  browser.sleep(4000); // DO NOT REMOVE, button clickable few seconds after DEPLOY click
};


module.exports.deployExistingApplication = function(applicationName) {
  cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
  goToApplicationDetailPage(applicationName, false);
  navigation.go('applications', 'deployment');
  var selected = cloudsCommon.selectApplicationCloud('testcloud');
  expect(selected).toBe(true); // testcloud is in the select
  browser.sleep(1000);

  // enter deployment topology properties : mock paas provider
  setMockPaasProperties(mockPaaSDeploymentProperties);

  var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
  browser.actions().click(deployButton).perform();
  browser.sleep(2000); // DO NOT REMOVE, button clickable few seconds after DEPLOY click

  navigation.go('applications', 'runtime');
};

module.exports.undeploy = function() {
  // we assume that we're on application details
  navigation.go('applications', 'deployment');
  var undeployButton = browser.element(by.id('btn-undeploy'));
  browser.actions().click(undeployButton).perform();
  browser.sleep(7000); // DO NOT REMOVE, wait for UNDEPLOY
};

function goToApplicationEnvironmentPageForApp(applicationName) {
  goToApplicationDetailPage(applicationName, false);
  navigation.go('applications', 'environments');
}
module.exports.goToApplicationEnvironmentPageForApp = goToApplicationEnvironmentPageForApp;

// create application environment
var createApplicationEnvironment = function(envName, envDescription, cloudSelectName, envTypeSelectName, appVersionName) {

  navigation.go('applications', 'environments');

  var btnNewApplicationEnv = browser.element(by.id('app-env-new-btn'));
  browser.actions().click(btnNewApplicationEnv).perform();

  element(by.model('environment.name')).sendKeys(envName);
  element(by.model('environment.description')).sendKeys(envDescription);

  if (typeof cloudSelectName !== 'undefined') {
    // cloudSelectNumber should start at 2 since the one at 1 is (no cloud) first ins the list
    var selectCloud = element(by.id('cloudid'));
    common.selectDropdownByText(selectCloud, cloudSelectName, 100);
  } else {
    console.error('You should have at least one cloud defined');
  }

  if (typeof envTypeSelectName !== 'undefined') {
    // envTypeSelectNumber should start at 2 since the one at 1 is (no envTypeSelectNumber) first ins the list
    var selectType = element(by.id('envtypelistid'));
    common.selectDropdownByText(selectType, envTypeSelectName, 100);
  } else {
    console.error('You should have at least one environment type defined');
  }

  if (typeof envTypeSelectName !== 'undefined') {
    var selectType = element(by.id('versionslistid'));
    common.selectDropdownByText(selectType, appVersionName, 100);
  } else {
    console.error('You should have at least one application version type defined');
  }

  // Create an App env
  var btnCreate = browser.element(by.id('btn-create'));
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();

};
module.exports.createApplicationEnvironment = createApplicationEnvironment;

function goToApplicationVersionPageForApp(applicationName) {
  goToApplicationDetailPage(applicationName, false);
  navigation.go('applications', 'versions');
}
module.exports.goToApplicationVersionPageForApp = goToApplicationVersionPageForApp;

var createApplicationVersion = function(version, description, selectTopology) {
  navigation.go('applications', 'versions');

  var btnNewApplicationVersion = browser.element(by.id('app-version-new-btn'));
  browser.actions().click(btnNewApplicationVersion).perform();

  element(by.model('versionId')).sendKeys(version);
  element(by.model('descId')).sendKeys(description);

  if (typeof selectTopology !== 'undefined') {
    var selectCloud = element(by.id('topologyId'));
    common.selectDropdownByText(selectCloud, selectTopology, 100);
  } else {
    console.error('Create an application version with an empty topology');
  }

  // Create an App env
  var btnCreate = browser.element(by.id('btn-create'));
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();

};
module.exports.createApplicationVersion = createApplicationVersion;

// select the environment
var selectApplicationEnvironment = function(envName) {
  var selectElement = element(by.id('environment-select'));
  var selectResult = common.selectDropdownByText(selectElement, envName, 1);
  return selectResult; // promise
};
module.exports.selectApplicationEnvironment = selectApplicationEnvironment;

var expectDeploymentPropertyValue = function(id, value, editableBoolean) {
  // editableBoolean => true > field is editable, false it's not
  var container = element(by.id(id));
  expect(container.isPresent()).toBe(true);
  expect(container.isDisplayed()).toBe(true);
  var editable = (editableBoolean === undefined || editableBoolean === '' || editableBoolean === null) ? false : editableBoolean; // if editableBoolean not defined, false
  var displayValue = {};
  if (editable) {
    displayValue = container.element(by.tagName('span'));
  } else {
    displayValue = container.element(by.tagName('em'));
  }
  expect(displayValue.isPresent()).toBeTruthy();
  displayValue.getText().then(function(spanText) {
    expect(spanText.toLowerCase()).toContain(value.toString().toLowerCase());
  });
};
module.exports.expectDeploymentPropertyValue = expectDeploymentPropertyValue;

// check output property / attribute on info or deployment page
// prerequisite : we're on application details menu
var expectOutputValue = function expectOutputValue(appPageState, environmentName, outputType, nodeId, instance, key, value) {

  // jump on the targeted page : info / deployment => default deplyoment
  var targetedPageStateId = appPageState || 'deployment';
  navigation.go('applications', targetedPageStateId);

  if (outputType === 'property') {
    instance = null;
  }

  // select the environment if "info" page
  if (targetedPageStateId === 'info') {
    environmentName = environmentName === null ? 'Environment' : environmentName;
  } else {
    // deployment page => may select an environment to check
    switchEnvironmentAndCloud(environmentName, null); // default cloud
  }

  // outputType : attribute / property
  var outputElementId = (instance !== null) ? outputType + '-' + nodeId + '-' + instance + '-' + key : outputType + '-' + nodeId + '-' + key;
  var outputElementToCheck = element(by.id(outputElementId));

  // check value
  expect(outputElementToCheck.isDisplayed()).toBe(true);
  outputElementToCheck.getText().then(function(spanText) {
    expect(spanText.toLowerCase()).toContain(value.toString().toLowerCase());
  });

};
module.exports.expectOutputValue = expectOutputValue;

var selectTopologyVersion = function selectTopologyVersion(appVersionName) {
  navigation.go('applications', 'topology');
  if (typeof appVersionName !== 'undefined') {
    var selectVersion = element(by.id('versionslistid'));
    common.selectDropdownByText(selectVersion, appVersionName, 100);
  } else {
    console.error('You should have at least one application version type defined');
  }
};
module.exports.selectTopologyVersion = selectTopologyVersion;
