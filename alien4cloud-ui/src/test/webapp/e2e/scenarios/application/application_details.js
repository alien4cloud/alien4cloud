/* global describe, it, element, by, expect, browser */
'use strict';

var setup = require('../../common/setup');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var applications = require('../../applications/applications');

var applicationsData = require(__dirname + '/_data/application_details/applications.json');
var applicationEnvironmentsData = require(__dirname + '/_data/application_details/applicationenvironments.json');
var applicationVersionsData = require(__dirname + '/_data/application_details/applicationversions.json');

// Tags definition
var goodTag = {
  'key': 'my_good_tag',
  'value': 'Whatever i want to add as value here...'
};
var goodTag2 = {
  'key': 'my_good_tag2',
  'value': 'Whatever i want to add as value here for ...'
};

describe('Topology templates details:', function() {
  it('beforeAll', function() {
    setup.setup();
    setup.index('application', 'application', applicationsData);
    setup.index('applicationenvironment', 'applicationenvironment', applicationEnvironmentsData);
    setup.index('applicationversion', 'applicationversion', applicationVersionsData);
    common.home();
    authentication.login('applicationManager');
  });

  it('Application manager should be able to rename an application', function() {
    var oldName = 'ApplicationTestRename';
    var newName = 'ApplicationTestRenameSuccess';
    applications.goToApplicationDetailPage(oldName);
    xedit.sendKeys('app-name', newName, false);
    expect(element(by.id('app-name')).isPresent()).toBe(true);
  });

  it('Application manager should not be able to rename an application with an existing name', function() {
    var oldName = 'ApplicationTestRenameFailed';
    var newName = 'ApplicationTestRenameSuccess';
    applications.goToApplicationDetailPage(oldName);
    xedit.sendKeys('app-name', newName, false);
    toaster.expectErrors();
    element(by.css('#app-name input')).sendKeys('0');
    toaster.dismissIfPresent();
  });

  it('Application manager should be able to edit the application description', function() {
    var currentAppName = 'ApplicationTestDescription';
    var newDescription = 'New brilliant description';
    applications.goToApplicationDetailPage(currentAppName);
    xedit.sendKeys('app-desc', newDescription, false, 'textarea');
    expect(element(by.binding('application.description')).getText()).toEqual(newDescription);
  });

  it('Application manager should be able to edit the application tags', function() {
    applications.goToApplicationDetailPage('ApplicationTestTags');
    var tags = element.all(by.repeater('tag in application.tags'));
    tags.count().then(function() {
      expect(tags.count()).toEqual(0);

      /* Add a new valid tag i should have the tags count +1 */
      element(by.model('newTag.key')).sendKeys(goodTag.key);
      element(by.model('newTag.val')).sendKeys(goodTag.key);
      var btnAddTag = browser.element(by.id('btn-add-tag'));
      btnAddTag.click();
      expect(tags.count()).toEqual(1);

      /* Add the same tag a second time : case update tag */
      element(by.model('newTag.key')).sendKeys(goodTag.key);
      element(by.model('newTag.val')).sendKeys(goodTag.key);
      btnAddTag.click();
      expect(tags.count()).toEqual(1);

      /* Add a second tag */
      element(by.model('newTag.key')).sendKeys(goodTag2.key);
      element(by.model('newTag.val')).sendKeys(goodTag2.key);
      btnAddTag.click();
      expect(tags.count()).toEqual(2);
    });
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
