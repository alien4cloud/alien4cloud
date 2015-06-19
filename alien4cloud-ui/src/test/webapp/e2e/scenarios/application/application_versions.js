/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');
var cloudsCommon = require('../../admin/clouds_common');

function assertCountVersions(expectedCount) {
  var versions = element.all(by.repeater('version in searchAppVersionResult'));
  expect(versions.count()).toEqual(expectedCount);
}

describe('Application versions', function() {
  var reset = true;
  var after = false;

  /* Before each spec in the tests suite */
  beforeEach(function() {
    if (reset) {
      reset = false;
      common.before();
      authentication.login('admin');
      cloudsCommon.goToCloudList();
      cloudsCommon.createNewCloud('testcloud');
      applications.createApplication('Alien', 'Great Application with application version...');
    }
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    if (after) {
      authentication.logout();
    }
  });

  it('should create an application and must have a default application version', function() {
    console.log('################# should create an application and must have a default application version');
    applications.goToApplicationVersionPageForApp('Alien');
    assertCountVersions(1);
  });

  it('should create an application version for an application', function() {
    console.log('################# should create an application version for an application');
    applications.createApplicationVersion('0.2.0', 'A new version for my application...');
    common.expectNoErrors();
    assertCountVersions(2);
  });

  it('should create an application version for an application with the topology of a previous app version', function() {
    console.log('################# should create an application version for an application with the topology of a previous app version');
    applications.createApplicationVersion('0.3.0', 'A new version for my application...', '0.1.0-SNAPSHOT');
    common.expectNoErrors();
    assertCountVersions(3);
  });

  it('should be able to delete an application environment', function() {
    console.log('################# should be able to delete a created environment');
    assertCountVersions(3);
    common.deleteWithConfirm('delete-version-0.2.0', true);
    common.expectNoErrors();
    assertCountVersions(2);
  });

  it('should not create an app version with a bad name', function() {
    console.log('################# should not create an app version with a bad name');
    var btnNewApplicationVersion = browser.element(by.id('app-version-new-btn'));
    browser.actions().click(btnNewApplicationVersion).perform();
    element(by.model('versionId')).sendKeys('0...1..0');

    var spanAlertPattern = browser.element(by.id('span-alert-pattern'));
    expect(spanAlertPattern.isPresent()).toBe(true);
    expect(spanAlertPattern.isDisplayed()).toBe(true);

    var btnCreate = browser.element(by.id('btn-create'));
    expect(btnCreate.isPresent()).toBe(true);
    expect(btnCreate.isEnabled()).toBe(false);

    var btnCancel = browser.element(by.id('btn-cancel'));
    browser.actions().click(btnCancel).perform();
  });

  it('should not rename an app version with a bad name', function() {
    after = true;
    console.log('################# should not rename an app version with a bad name');
    common.sendValueToXEditable('td-0.1.0-SNAPSHOT', '0.1.', false);
    common.expectErrors();
    browser.sleep(5000); // DO NOT REMOVE, we need to send a valid value to the editable text
    element(by.css('#td-0\\.1\\.0-SNAPSHOT input')).sendKeys('0');
    assertCountVersions(2);
  });
});
