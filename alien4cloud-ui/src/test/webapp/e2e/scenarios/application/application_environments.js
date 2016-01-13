/* global it, by, expect, element */
'use strict';

var setup = require('../../common/setup');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var applications = require('../../applications/applications');
var applicationsEnvironments = require('../../applications/applications_environments');

function assertCountEnvironment(expectedCount) {
  var environments = element.all(by.repeater('environment in searchAppEnvResult'));
  expect(environments.count()).toEqual(expectedCount);
}

describe('Application environments', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('applicationManager');
    applications.goToApplicationDetailPage('AlienUITest');
    common.click(by.id('am.applications.detail.environments'));
  });

  it('should create an application and must have a default application environment.', function() {
    assertCountEnvironment(1);
  });

  it('should create an application environment for a new application.', function() {
    applicationsEnvironments.createApplicationEnvironment('ENV', 'A new environment for my application...', applicationsEnvironments.environmentTypes.dev, '0.1.0-SNAPSHOT');
    toaster.expectNoErrors();
    assertCountEnvironment(2);
  });

  it('should be able to delete an application environment.', function() {
    assertCountEnvironment(2);
    common.deleteWithConfirm('delete-env_ENV', true);
    toaster.expectNoErrors();
    assertCountEnvironment(1);
  });

  it('should reject a new application environment if an application environment with the same name already exist.', function() {
    applicationsEnvironments.createApplicationEnvironment('Environment', 'A new environment whith an existing name', applicationsEnvironments.environmentTypes.dev, '0.1.0-SNAPSHOT');
    toaster.expectErrors();
    toaster.dismissIfPresent();
    assertCountEnvironment(1);
  });

  it('should failed to remove the last new application environment.', function() {
    common.deleteWithConfirm('delete-env_Environment', true);
    toaster.expectErrors();
    toaster.dismissIfPresent();
    assertCountEnvironment(1);
  });

  it('should failed to rename if an application environment with the same name already exist.', function() {
    applicationsEnvironments.createApplicationEnvironment('ENV', 'A new environment for my application...', applicationsEnvironments.environmentTypes.dev, '0.1.0-SNAPSHOT');
    toaster.expectNoErrors();
    assertCountEnvironment(2);
    xedit.sendKeys('ENV-name-td', 'Environment', false);
    toaster.expectErrors();
    element(by.css('#ENV-name-td input')).sendKeys('2');
    toaster.dismissIfPresent();
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
