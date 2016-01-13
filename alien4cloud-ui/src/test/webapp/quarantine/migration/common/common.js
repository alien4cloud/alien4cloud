/* global browser, protractor, by, element */

'use strict';


var dismissAlert = function() { // toast-close-button
  element(by.css('.toast-close-button')).click();
  browser.waitForAngular();
};
module.exports.dismissAlert = dismissAlert;





// Get URL by number
var getUrlElement = function(url, elementIndex) {
  var urlElements = url.split('#');
  var secondPart = urlElements[1];
  if (elementIndex !== '' && elementIndex >= 0 && elementIndex <= secondPart.length) {
    return secondPart.split('/')[elementIndex];
  }
  return secondPart.split('/');
};
module.exports.getUrlElement = getUrlElement;

var abortXEditable = function(id, type) {
  var container = element(by.id(id));
  var editForm = container.element(by.tagName('form'));
  var editInput;
  if (type) {
    editInput = editForm.element(by.tagName(type));
  } else {
    editInput = editForm.element(by.tagName('input'));
  }
  editInput.sendKeys(protractor.Key.ESCAPE);
  browser.waitForAngular();
};
module.exports.abortXEditable = abortXEditable;



var expectValueFromToscaProperty = function(id, value) {
  // For the moment only consider x-editable.log
  expectValueFromXEditable('p_' + id, value);
};
module.exports.expectValueFromToscaProperty = expectValueFromToscaProperty;

/* Handeling error assert */
var expectNoErrors = function() {
  expectToasterDivClass('error', false);
};
module.exports.expectNoErrors = expectNoErrors;

var expectErrors = function() {
  expect(element.all(by.repeater('toaster in toasters')).count()).toBeGreaterThan(0);
  expectToasterDivClass('error', true);
};
module.exports.expectErrors = expectErrors;

var expectSuccess = function() {
  expect(element.all(by.repeater('toaster in toasters')).count()).toBeGreaterThan(0);
  expectToasterDivClass('success', true);
};
module.exports.expectSuccess = expectSuccess;

// not exported
var expectToasterDivClass = function(clazz, exists) {
  expect(browser.isElementPresent(by.className('toast-' + clazz))).toBe(exists);
};

var expectTitleMessage = function(code) {
  element(by.css('.toast-title')).getText().then(function(fullErrorMessage) {
    expect(fullErrorMessage).toContain(code);
  });
};
module.exports.expectTitleMessage = expectTitleMessage;

var expectMessageContent = function(text) {
  // check if the error toaster body contains 'text"
  element(by.css('.toast-message')).getText().then(function(fullErrorMessage) {
    expect(fullErrorMessage).toContain(text);
  });
};
module.exports.expectMessageContent = expectMessageContent;





var toggleDisplayFacetManagementButton = function toggleDisplayFacetManagementButton() {
  element(by.id('displayFacetManagement')).isDisplayed().then(function(isDisplay) {
    if (isDisplay) {
      element(by.id('displayFacetManagement')).click();
    }
  });
  browser.sleep(1000); // DO NOT REMOVE, wait few seconds for the ui to be ready
};

var removeAllFacetFilters = function removeAllFacetFilters() {
  toggleDisplayFacetManagementButton();
  element.all(by.repeater('filter in facetFilters')).each(function(facet) {
    facet.element(by.tagName('a')).click();
  });
  toggleDisplayFacetManagementButton();
  browser.waitForAngular();
};
module.exports.removeAllFacetFilters = removeAllFacetFilters;

var sendXEditableWithConfirm = function(id, newValue) {
  var container = element(by.id(id));
  var span = container.element(by.css('.editable-click'));
  span.element(by.tagName('i')).click();
  var editForm = container.element(by.tagName('form'));
  var editInput = editForm.element(by.tagName('input'));
  editInput.clear();
  editInput.sendKeys(newValue);
};

module.exports.sendXEditableWithConfirm = sendXEditableWithConfirm;
