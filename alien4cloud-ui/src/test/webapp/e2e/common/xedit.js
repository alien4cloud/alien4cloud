/* global browser, by, expect */
'use strict';

var common = require('./common');

var sendKeys = function(id, value, withAutoCompletion, type) {
  sendKeysWithSelector(by.id(id), value, withAutoCompletion, type);
};

var sendKeysWithSelector = function(selector, value, withAutoCompletion, type) {
  // Find the container of x-editable
  var container = common.element(selector);
  var span = common.element(by.css('.editable-click'), container);
  // click on the span of x-editable to trigger input
  if (type === 'tosca') {
    common.click(by.tagName('span'), span);
  } else {
    common.click(by.tagName('i'), span);
  }

  var editForm = common.element(by.tagName('form'), container);
  var editInput;
  if (type && type !== 'tosca') {
    editInput = common.element(by.tagName(type), editForm);
  } else {
    editInput = common.element(by.tagName('input'), editForm);
  }
  editInput.clear();
  editInput.sendKeys(value);
  browser.waitForAngular();

  if (withAutoCompletion) {
    common.element(by.tagName('ul'), editForm).all(by.tagName('li')).then(function(autoCompletionProposals) {
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
module.exports.sendKeys = sendKeys;
module.exports.sendKeysWithSelector = sendKeysWithSelector;

var xeditExpect = function(id, value) {
  xeditExpectWithSelector(by.id(id), value);
};

var xeditExpectWithSelector = function(selector, value) {
  var container = common.element(selector);
  var span = common.element(by.tagName('span'), container);
  if (value === '') { // toContain failed on empty string
    span.getText().then(function(spanText) {
      expect(spanText).toBe('');
    });
  } else {
    span.getText().then(function(spanText) {
      expect(spanText.toLowerCase()).toContain(value.toString().toLowerCase());
    });
  }
};
module.exports.expect = xeditExpect;
module.exports.expectWithSelector = xeditExpectWithSelector;
