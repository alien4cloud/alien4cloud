/* global browser, by, expect */

'use strict';

var common = require('./common');

var sendKeys = function(id, value, withAutoCompletion, type) {
  // Find the container of x-editable
  var container = common.element(by.id(id));
  var span = common.element(by.css('.editable-click'), container);
  // click on the span of x-editable to trigger input
  common.click(by.tagName('i'), span);

  var editForm = common.element(by.tagName('form'), container);
  var editInput;
  if (type) {
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

var xeditExpect = function(id, value) {
  var container = common.element(by.id(id));
  var span = common.element(by.tagName('span'), container);
  span.getText().then(function(spanText) {
    expect(spanText.toLowerCase()).toContain(value.toString().toLowerCase());
  });
};
module.exports.expect = xeditExpect;
