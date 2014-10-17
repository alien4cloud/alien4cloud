'use strict';

var common = require('./common');
var rolesCommon = require('./roles_common.js');

/* Vars */
var user = {
  'username': 'user',
  'password': 'user'
};
var componentManager = {
  'username': 'componentManager',
  'password': 'componentManager'
};
var componentBrowser = {
  'username': 'componentBrowser',
  'password': 'componentBrowser'
};
var applicationManager = {
  'username': 'applicationManager',
  'password': 'applicationManager'
};
var appManager = {
  'username': 'appManager',
  'password': 'appManager'
};
var admin = {
  'username': 'admin',
  'password': 'admin'
};
var badUser = {
  'username': 'bad',
  'password': 'user'
};
var ptor;

// Tags definition
var goodTag = {
  'key': 'my_good_tag',
  'value': 'Whatever i want to add as value here...'
};

var goodTag2 = {
  'key': 'my_good_tag2',
  'value': 'Whatever i want to add as value here for ...'
};

/* Jump to application list and select the 'elementNumber' element */
var gotoApplicationDetails = function(elementNumber) {
  common.goToApplicationSearchPage();
  var applications = element.all(by.repeater('application in searchResult.data.data'));
  expect(applications.count()).toBeGreaterThan(elementNumber);

  // Select the first line and click on details button
  var firstElement = applications.get(elementNumber);
  browser.actions().click(firstElement).perform();
  browser.waitForAngular();
};

/* Jump to components list and select the 'elementNumber' element */
var gotoComponentDetails = function(elementNumber) {
  common.goToComponentsSearchPage();
  var components = element.all(by.repeater('component in searchResult.data.data'));
  expect(components.count()).toBeGreaterThan(elementNumber);

  // Select the first line and click on details button
  var firstElement = components.get(elementNumber);
  browser.actions().click(firstElement).perform();
};

describe('Permissions tests on APPLICATIONS', function() {
  /* Before each spec in the tests suite */
  beforeEach(function() {
    /* Go on home page */
    common.before();
    ptor = protractor.getInstance();
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });


  it('should have application list when authenticated with role : APPLICATIONS_MANAGER', function() {
    console.log('################# should have application list when authenticated with role : APPLICATIONS_MANAGER');

    // Login with bad user (can't handle applications)
    common.login(applicationManager.username, applicationManager.password);
    common.goToApplicationSearchPage();
    var newAppName = 'Alien-' + applicationManager.username;

    // testing error messages
    common.expectNoErrors();
    // var errorsFoundPromise = element.all(by.binding('error.data')).then(function(errorsFound) {
    //   expect(errorsFound.length).toEqual(0);
    // });

    // Can add a new application
    common.createApplication(newAppName, 'Great Application by ' + applicationManager.username);

    // Check the new application list : should have an application name with
    // "-applicationManager"
    var applications = element.all(by.repeater('application in searchResult.data.data'));

    expect(applications.count()).toBeGreaterThan(0);
    var lastElement = applications.last();
    expect(lastElement.element(by.binding('application.name')).getText()).toEqual(newAppName);

    // Go on last application to check rights
    applications.count().then(function(nbApps) {
      gotoApplicationDetails(nbApps - 1); // last application id
      // you should not have edit form
      expect(element(by.name('formAddTag')).isDisplayed()).toBe(true);

      // you cannot edit existing tags
      var tags = element.all(by.repeater('(tag, tagValue) in application.tags'));

      tags.count().then(function(nbTags) {

        // add at least one tag is no tags
        if (nbTags >= 0) {

          /* Add a new valid tag i should have the tags count +1 */
          element(by.model('newTag.key')).sendKeys(goodTag.key);
          element(by.model('newTag.val')).sendKeys(goodTag.key);

          /* Click to add the new element */
          var btnAddTag = browser.element(by.id('btn-add-tag'));
          browser.actions().click(btnAddTag).perform();

          /* Add a second tag */
          element(by.model('newTag.key')).sendKeys(goodTag2.key);
          element(by.model('newTag.val')).sendKeys(goodTag2.key);

          /* Click to add the good tag twice */
          browser.actions().click(btnAddTag).perform();

        }

        // you can edit existing tags
        var tags = element.all(by.repeater('(tag, tagValue) in application.tags'));
        tags.count().then(function(nbTags) {
          if (nbTags > 0) {
            // check all tags element : shoud be editable
            element.all(by.repeater('(tag, tagValue) in application.tags')).each(function(tag) {
              expect(tag.element(by.css('span[editable-text]')).isDisplayed()).toBe(true);
            });
          }
        });
        // end : tags editable tests

      });
    });

  });

  it('should not have edit rights when you have no application role', function() {

    console.log('################# should not have edit rights when you have no application role');
    common.login(appManager.username, appManager.password);

    // Create an application with user applicationManager
    common.reLogin(applicationManager.username, applicationManager.password);
    common.goToApplicationSearchPage();
    // Can add a new application
    common.createApplication('Alien-' + applicationManager.username, 'Great Application by ' + applicationManager.username);
    element(by.xpath('//ul[contains(@class, "nav-tabs")]/li[2]/a')).click();
    rolesCommon.editUserRole(appManager.username, common.appRoles.appUser);

    // Check that another user can see the new app but cannot edit
    common.reLogin(appManager.username, appManager.password);
    common.goToApplicationSearchPage();

    // Last application was added by applicationManager as owner
    // now you're logged as appManager
    var applications = element.all(by.repeater('application in searchResult.data.data'));

    applications.count().then(function(nbApps) {
      expect(nbApps).toBeGreaterThan(0);
      gotoApplicationDetails(nbApps - 1); // last application id
      // you should not have edit form
      expect(element(by.name('formAddTag')).isDisplayed()).toBe(false);

      // you cannot edit existing tags
      var tags = element.all(by.repeater('(tag, tagValue) in application.tags'));
      tags.count().then(function(nbTags) {
        if (nbTags > 0) {
          // check all tags element : shoud not be editable
          element.all(by.repeater('(tag, tagValue) in application.tags')).each(function(tag) {
            expect(tag.element(by.css('span[editable-text]')).isDisplayed()).toBe(false);
          });
        }
      });
    });
  });

  it('should not have rights to edit a component as : COMPONENTS_BROWSER', function() {
    console.log('################# should not have rights to edit a component as : COMPONENTS_BROWSER');

    // Upload by the manager
    common.login(componentManager.username, componentManager.password);
    common.goToComponentsSearchPage();
    common.uploadTestComponents();
    common.logout();

    // Login as a browser
    common.login(componentBrowser.username, componentBrowser.password);
    gotoComponentDetails(0);

    // you cannot edit existing tags
    var tags = element.all(by.repeater('(tag, tagValue) in component.tags'));
    tags.count().then(function(nbTags) {
      if (nbTags > 0) {
        // check all tags element : shoud not be editable
        element.all(by.repeater('(tag, tagValue) in component.tags')).each(function(tag) {
          expect(tag.element(by.css('span[editable-text]')).isDisplayed()).toBe(false);
        });
      }
    });
  });

});
