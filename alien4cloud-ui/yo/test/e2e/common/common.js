/* global protractor, by, element */

'use strict';

var cleanup = require('./cleanup');
var navigation = require('./navigation');
var authentication = require('../authentication/authentication');
var SCREENSHOT = require('./screenshot');

var dismissAlert = function() { // toast-close-button
  element(by.css('.toast-close-button')).click();
  browser.waitForAngular();
};
module.exports.dismissAlert = dismissAlert;

var flow = protractor.promise.controlFlow();

var dismissAlertIfPresent = function() { // toast-close-button
  flow.execute(function() {
    var closeAlertButton = element(by.css('.toast-close-button'));
    closeAlertButton.click();
    browser.waitForAngular();
  }).then(function(value) {}, function(error) {
    return true;
  });
};
module.exports.dismissAlertIfPresent = dismissAlertIfPresent;

module.exports.before = function() {
  // cleanup ElasticSearch and alien folders.
  cleanup.cleanup();
  navigation.home();
};

module.exports.after = function() {
  authentication.logout();
};

// Loading and exposing languages for tests
var frLanguage = require(__dirname + '/../../../app/data/languages/locale-fr-fr.json');
var usLanguage = require(__dirname + '/../../../app/data/languages/locale-en-us.json');

module.exports.frLanguage = frLanguage;
module.exports.usLanguage = usLanguage;

module.exports.uploadFile = function(path) {
  browser.driver.executeScript('var $scope = angular.element($(\'#fileUpload\')).scope(); $scope.dropSupported=false; $scope.$apply();').then(function() {
    browser.waitForAngular();
    var fileInput = browser.element(by.css('input[type="file"]'));
    fileInput.sendKeys(path);
    SCREENSHOT.takeScreenShot('upload-components');
    browser.waitForAngular();
  });
  browser.waitForAngular();
};

module.exports.chooseSelectOption = function(selectElement, value) {
  selectElement.all(by.tagName('option')).then(function() {
    var desiredOption;
    selectElement.all(by.tagName('option'))
      .then(function findMatchingOption(options) {
        options.some(function(option) {
          option.getText().then(function doesOptionMatch(text) {
            if (text.trim() === value) {
              desiredOption = option;
              return true;
            }
          });
        });
      })
      .then(function clickOption() {
        if (desiredOption) {
          desiredOption.click();
        }
      });
  });
};

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

var sendValueToXEditable = function(id, value, withAutoCompletion, type) {
  // Find the container of x-editable
  var container = element(by.id(id));
  expect(container.isPresent()).toBe(true);
  expect(container.isDisplayed()).toBe(true);

  var span = container.element(by.css('.editable-click'));
  expect(span.isDisplayed()).toBe(true);
  // click on the span of x-editable to trigger input
  span.element(by.tagName('i')).click();
  var editForm = container.element(by.tagName('form')); // this fucking shit doesn't work on firefox !
  var editInput;
  if (type) {
    editInput = editForm.element(by.tagName(type));
  } else {
    editInput = editForm.element(by.tagName('input'));
  }
  editInput.clear();
  editInput.sendKeys(value);
  browser.waitForAngular();
  if (withAutoCompletion) {
    editForm.element(by.tagName('ul')).all(by.tagName('li')).then(function(autoCompletionProposals) {
      var firstProposal = autoCompletionProposals[0];
      browser.actions().click(firstProposal).perform();
      browser.waitForAngular();
      editForm.submit();
      browser.waitForAngular();
      span.getText().then(function(spanText) {
        expect(spanText.toLowerCase()).toContain(value);
      });
    });
  } else {
    editForm.submit();
    browser.waitForAngular();
  }
};
module.exports.sendValueToXEditable = sendValueToXEditable;

var sendValueToToscaProperty = function(id, value) {
  // For the moment only consider x-editable
  sendValueToXEditable('p_' + id, value);
};

module.exports.sendValueToToscaProperty = sendValueToToscaProperty;

var expectValueFromXEditable = function(id, value) {
  var container = element(by.id(id));
  expect(container.isPresent()).toBe(true);
  expect(container.isDisplayed()).toBe(true);
  var span = container.element(by.tagName('span'));
  expect(span.isDisplayed()).toBe(true);
  span.getText().then(function(spanText) {
    expect(spanText.toLowerCase()).toContain(value.toString().toLowerCase());
  });
};

module.exports.expectValueFromXEditable = expectValueFromXEditable;

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

// For a SELECT element : select by value
// WARNING : no error is the item is not found
var selectDropdownByText = function selectOption(selectElement, item, milliseconds) {
  var desiredOption = null;
  var deferred = protractor.promise.defer();
  selectElement.all(by.tagName('option'))
    .then(function findMatchingOption(options) {
      options.some(function(option) {
        option.getText().then(function doesOptionMatch(text) {
          if (text.indexOf(item) !== -1) {
            desiredOption = option;
            return true;
          }
        });
      });
    })
    .then(function clickOption() {
      var itemFoundInSelect = false;
      if (desiredOption) {
        desiredOption.click();
        itemFoundInSelect = true;
      } else {
        console.error('Desired item {', item, '} not found in the select');
      }
      deferred.fulfill(itemFoundInSelect);
    });

  // waiting time after selection
  if (typeof milliseconds !== 'undefined') {
    browser.sleep(milliseconds);
  }
  return deferred.promise;

};
module.exports.selectDropdownByText = selectDropdownByText;

// For a SELECT element : return select count
var selectCount = function selectCount(selectId) {
  var deferred = protractor.promise.defer();
  var selectOptions = element.all(by.css('select[id="' + selectId + '"] option'));
  selectOptions.count().then(function(count) {
    deferred.fulfill(count);
  });
  return deferred.promise;
};
module.exports.selectCount = selectCount;

// Remove an element with confirm popover
// The button/a generated by <delete-confirm/> directive has no id
// we have to get the directive child <a> to click on it (or not)
var deleteWithConfirm = function(deleteConfirmDirectiveId, confirm) {
  var deleteConfirm = element(by.id(deleteConfirmDirectiveId));
  deleteConfirm.element(by.tagName('a')).click();
  // get popover div and grab the Yes / No buttons
  var divPopover = element(by.css('.popover'));
  var buttonToClick = null;
  if (confirm === true) { // confirm deletion
    buttonToClick = divPopover.element(by.css('.btn-success'));
  } else { // cancel
    buttonToClick = divPopover.element(by.css('.btn-danger'));
  }
  browser.actions().click(buttonToClick).perform();
};
module.exports.deleteWithConfirm = deleteWithConfirm;

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

var slideXEditableTo = function(id, newValue){
  var scaleEditableInput = element(by.id(id));
  scaleEditableInput.getText().then(function(text){
    var oldValue = parseInt(text.trim());
    scaleEditableInput.click();
    var editForm = scaleEditableInput.element(by.tagName('form'));
    var slider = editForm.element(by.tagName('input'));
    var direction = oldValue < newValue ? protractor.Key.ARROW_RIGHT : protractor.Key.ARROW_LEFT;
    var repeat = Math.abs(newValue - oldValue);
    slider.click();
    while(repeat>0){
      slider.sendKeys(direction);
      repeat--;
    }
  });
};

module.exports.slideXEditableTo = slideXEditableTo;
