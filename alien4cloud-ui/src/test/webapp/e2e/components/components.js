/* global by */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.components'));
};
module.exports.go = go;

module.exports.tags = {
  goodTag: {
    key: 'my_good_tag',
    value: 'Whatever i want to add as value here...'
  },
  goodTag2: {
    key: 'my_good_tag2',
    value: 'Whatever i want to add as value here for ...'
  },
  badTag: {
    key: 'my_good*tag',
    value: 'This tag should not be added to tag list with *...'
  }
};

var checkRecommanded = function(recommended, capabilityRow) {
  expect(capabilityRow.element(by.css('.alert-success')).isPresent()).toBe(recommended);
  expect(capabilityRow.element(by.css('a.btn-success')).isDisplayed()).toBe(!recommended);
  expect(capabilityRow.element(by.css('a.btn-danger')).isDisplayed()).toBe(recommended);
};

var findCapabilityRow = function(testedCapabilityId) {
  return browser.element(by.id(testedCapabilityId));
};

var flagComponentAsRecommanded = function(component, testedCapability) {
  components.goToComponentDetailPage(component.id);
  expect(element.all(by.binding('component.elementId')).first().getText()).toContain(component.elementId);
  expect(element(by.binding('component.archiveVersion')).getText()).toContain(component.archiveVersion);

  var firstCapaRow = findCapabilityRow(testedCapability);
  expect(firstCapaRow.getText()).toContain(testedCapability);
  expect(firstCapaRow.isElementPresent(by.css('a.btn-success'))).toBe(true);
  checkRecommanded(false, firstCapaRow);
  var recommendButton = firstCapaRow.element(by.css('a.btn-success'));
  // recommend for this capability
  recommendButton.click();
};
