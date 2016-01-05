/* global browser, by, element, expect, protractor */
'use strict';

// Private
var expectToasterDivClass = function(clazz, exists) {
  expect(browser.isElementPresent(by.className('toast-' + clazz))).toBe(exists);
};

module.exports.expectSuccess = function() {
  expect(element.all(by.repeater('toaster in toasters')).count()).toBeGreaterThan(0);
  expectToasterDivClass('success', true);
};

module.exports.expectNoErrors = function() {
  expectToasterDivClass('error', false);
};

module.exports.expectErrors = function() {
  expect(element.all(by.repeater('toaster in toasters')).count()).toBeGreaterThan(0);
  expectToasterDivClass('error', true);
};

var flow = protractor.promise.controlFlow();
module.exports.dismissIfPresent = function() { // toast-close-button
  flow.execute(function() {
    var closeAlertButton = element(by.css('.toast-close-button'));
    closeAlertButton.click();
    browser.waitForAngular();
  }).then(function() {
  }, function() {
    return true;
  });
};

// check if the toaster body message contains 'text"
module.exports.expectMessageToContain = function(text) {
  element(by.css('.toast-message')).getText().then(function(fullErrorMessage) {
    expect(fullErrorMessage).toContain(text);
  });
};
