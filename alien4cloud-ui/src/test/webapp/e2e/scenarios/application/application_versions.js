/* global by, describe, element, it, browser, expect */
'use strict';

var setup = require('../../common/setup');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var applications = require('../../applications/applications');

function assertCountVersions(expectedCount) {
  var versions = element.all(by.repeater('version in searchAppVersionResult'));
  expect(versions.count()).toEqual(expectedCount);
}

describe('Application versions', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('applicationManager');
    applications.goToApplicationDetailPage('AlienUITest');
    common.click(by.id('am.applications.detail.versions'));
  });

  it('should create an application and must have a default application version', function() {
    assertCountVersions(1);
  });

  it('should create an application version for an application', function() {
    applications.createApplicationVersion('0.2.0', 'A new version for my application...');
    toaster.expectNoErrors();
    assertCountVersions(2);
  });

  it('should create an application version for an application with the topology of a previous app version', function() {
    applications.createApplicationVersion('0.3.0', 'A new version for my application...', '0.1.0-SNAPSHOT');
    toaster.expectNoErrors();
    assertCountVersions(3);
  });

  it('should be able to delete an application environment', function() {
    assertCountVersions(3);
    common.deleteWithConfirm('delete-version-0.2.0', true);
    toaster.expectNoErrors();
    assertCountVersions(2);
  });

  it('should not create an app version with a bad name', function() {
    common.click(by.id('app-version-new-btn'));
    element(by.model('versionId')).sendKeys('0...1..0');

    var spanAlertPattern = browser.element(by.id('span-alert-pattern'));
    expect(spanAlertPattern.isPresent()).toBe(true);
    expect(spanAlertPattern.isDisplayed()).toBe(true);

    var btnCreate = browser.element(by.id('btn-create'));
    expect(btnCreate.isPresent()).toBe(true);
    expect(btnCreate.isEnabled()).toBe(false);

    common.click(by.id('btn-cancel'));
  });

  it('should not rename an app version with a bad name', function() {
    xedit.sendKeys('td-0.1.0-SNAPSHOT', '0.1.', false);
    toaster.expectErrors();
    element(by.css('#td-0\\.1\\.0-SNAPSHOT input')).sendKeys('0');
    toaster.dismissIfPresent();
    assertCountVersions(2);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
