/* global element, by */
'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var applications = require('../applications/applications');

describe('Quick search', function() {
  beforeEach(function() {
    common.before();
  });

  afterEach(function() {
    common.after();
  });

  var checkQuickSearchForm = function(recommended) {
    expect(element(by.model('itemSelected')).isDisplayed()).toBe(recommended);
  };

  var uploadData = function() {
    authentication.login('admin');

    authentication.reLogin('applicationManager');
    applications.createApplication('Compute-application', '');
    applications.createApplication('Root-application', '');
    applications.createApplication('Storage-application', '');
    authentication.logout();
  };

  it('should be able to authenticate with any role and see the quicksearch form', function() {
    console.log('################# should be able to authenticate with any role and see the quicksearch form');
    checkQuickSearchForm(false);
    uploadData();
    // login
    for (var userKey in authentication.users) {
      if (authentication.users.hasOwnProperty(userKey) && authentication.users[userKey].registered) { // all registered users should be able to view the quick search.
        authentication.login(userKey);
        checkQuickSearchForm(true);
        authentication.logout();
      }
    }
    authentication.login('admin');
  });

  it('When authenticate with a role, should only see allowed items [components, application, or both]', function() {

    console.log('################# When authenticate with a role, should only see allowed items [components, application, or both]. ');
    // creating applications as applicationManager
    uploadData();

    var quicksearchWord = 'compute';
    var quicksearchWordApplication = 'application';
    var computeAppName = 'Compute-application';
    var rootAppName = 'Root-application';
    var storageAppName = 'Compute-application';

    // case component browser : shouldn't see applications created by applicationManager unless he has rights
    authentication.login('componentBrowser');
    var qsInput = element(by.model('itemSelected'));
    qsInput.sendKeys(quicksearchWord);
    // expect to have the typeahead div displayed
    var typeaheadUl = element(by.css('ul[typeahead-popup]'));
    expect(typeaheadUl.isDisplayed()).toBe(true);

    // expect the list of found elements to have less than 10 items
    var searchedItems = element.all(by.repeater('match in matches'));
    expect(searchedItems.count()).toBeLessThan(11);

    // expect not to have an application displayed: checking for displayed names
    expect(typeaheadUl.getText()).not.toContain(computeAppName);

    authentication.logout();

    // case applicationManager : he created the 3 applications, he must see them in the quicksearch with keywork application
    authentication.login('applicationManager');
    qsInput.clear();
    qsInput.sendKeys(quicksearchWordApplication);
    // expect to have the typeahead div displayed
    typeaheadUl = element(by.css('ul[typeahead-popup]'));
    expect(typeaheadUl.isDisplayed()).toBe(true);
    // expect the list of found elements to have less than 10 items
    searchedItems = element.all(by.repeater('match in matches'));
    expect(searchedItems.count()).toEqual(13);

    // expect to have an application displayed: checking for displayed names
    expect(typeaheadUl.getText()).toContain(computeAppName);
    expect(typeaheadUl.getText()).toContain(rootAppName);
    expect(typeaheadUl.getText()).toContain(storageAppName);

    authentication.logout();

    // case admin : should see all
    authentication.login('admin');
    qsInput.clear();
    qsInput.sendKeys(quicksearchWordApplication);
    // expect to have the typeahead div displayed
    typeaheadUl = element(by.css('ul[typeahead-popup]'));
    expect(typeaheadUl.isDisplayed()).toBe(true);

    // expect the list of found elements to have less than 10 items
    searchedItems = element.all(by.repeater('match in matches'));
    expect(searchedItems.count()).toBeGreaterThan(1);

    // should see applications created by applicationManager
    expect(typeaheadUl.getText()).toContain(computeAppName);
    expect(typeaheadUl.getText()).toContain(rootAppName);
    expect(typeaheadUl.getText()).toContain(storageAppName);
  });

  it('After typing a quicksearch, should be able to see details of a selected item', function() {

    console.log('################# After typing a quicksearch, should be able to see details of a selected item. ');
    uploadData();
    var quicksearchWord = 'compute';
    var computeAppName = 'Compute-application';

    // should find an application to click on it and see details
    authentication.login('applicationManager');
    var qsInput = element(by.model('itemSelected'));
    qsInput.clear();
    qsInput.sendKeys(computeAppName);
    var typeaheadUl = element(by.css('ul[typeahead-popup]'));
    var item = typeaheadUl.element(by.tagName('li'));
    item.click();
    expect(browser.element(by.binding('application.name')).getText()).toEqual(computeAppName);

    authentication.logout();

    // should find a component at first search result
    authentication.login('applicationManager');
    qsInput.clear();
    qsInput.sendKeys(quicksearchWord);
    common.ptor.sleep(1000);
    item = typeaheadUl.element(by.tagName('li'));
    item.getText().then(function(text) {
      item.click();
      expect(element(by.binding('component.elementId')).getText()).toContain(text.trim());
    });

  });

});