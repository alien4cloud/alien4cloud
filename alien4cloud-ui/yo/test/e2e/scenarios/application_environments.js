/* global by, element */
'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var tagConfigCommon = require('../admin/metaprops_configuration_common');
var applications = require('../applications/applications');
var cloudsCommon = require('../admin/clouds_common');
var rolesCommon = require('../common/roles_common');

function assertCountEnvironment(expectedCount) {
  var environments = element.all(by.repeater('environment in searchAppEnvResult'));
  expect(environments.count()).toEqual(expectedCount);
};

function assertEnvTypeForEnvironment(idEnvName, expectedEnvType) {
  expect(element(by.id(idEnvName + '-envlistid')).$('option:checked').getText()).toEqual(expectedEnvType);
};

describe('Application environments', function() {

  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    authentication.login('admin');
    // I create a new cloud
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud');
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();
    // authentication.reLogin('applicationManager');
    // cloudsCommon.giveRightsOnCloudToUser('testcloud', 'applicationManager', rolesCommon.cloudRoles.cloudDeployer);
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should create an application and must have a default application environment', function() {
    console.log('################# should create an application and must have a default application environment');
    applications.createApplication('Alien', 'Great Application with application environment to perform deployments...');
    applications.goToApplicationEnvironmentPageForApp('Alien');
    assertCountEnvironment(1);
    assertEnvTypeForEnvironment('Environment', 'OTHER');
  });

  it('should create an application environment for a new application', function() {
    console.log('################# should create an application environment for a new application');
    applications.createApplication('Alien', 'Great Application with application environment to perform deployments...');
    applications.goToApplicationEnvironmentPageForApp('Alien');
    applications.createApplicationEnvironment('MyAppEnvironment', 'A new environment for my application...', 'testcloud', applications.environments_type.dev, '0.1.0-SNAPSHOT');
    common.expectNoErrors();
    assertCountEnvironment(2);
  });

  it('should be able to delete an application environment', function() {
    console.log('################# should be able to delete a created environment');
    applications.createApplication('Alien', 'Great Application with application environment to perform deployments...');
    applications.goToApplicationEnvironmentPageForApp('Alien');
    applications.createApplicationEnvironment('ENV', 'A new environment for my application...', 'testcloud', applications.environments_type.dev, '0.1.0-SNAPSHOT');
    common.expectNoErrors();
    assertCountEnvironment(2);
    common.deleteWithConfirm('delete-env_ENV', true);
    common.expectNoErrors();
    assertCountEnvironment(1);
  });

  it('should fail when i create a new application environment with bad cloud name', function() {
    console.log('################# should fail when i create a new application environment with bad cloud name');
    applications.createApplication('Alien', 'Great Application with application environment to perform deployments with bad cloud name...');
    applications.goToApplicationEnvironmentPageForApp('Alien');
    applications.createApplicationEnvironment('MyAppEnvironment', 'A new environment for my application...', 'badCloudName', applications.environments_type.dev, '0.1.0-SNAPSHOT');
    common.expectNoErrors();
    assertCountEnvironment(1);
  });

});
