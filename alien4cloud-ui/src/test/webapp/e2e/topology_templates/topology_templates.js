/* global by, expect, element */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.topologytemplates'));
};
module.exports.go = go;

module.exports.create = function(name, description, cancel) {
  go();
  common.click(by.id('btn-add-template'));
  // assert that the create button is disabled when there is no name.
  var createButton = common.element(by.id('modal-create-button'));
  expect(createButton.getAttribute('disabled')).toEqual('true');
  common.sendKeys(by.id('topo-template-name'), name);
  common.sendKeys(by.id('topo-template-desc'), description);
  if (cancel) {
    common.click(by.id('modal-cancel-button'));
  } else {
    common.click(by.id('modal-create-button'));
  }
};

var searchTopologyTemplate = function(templateName) {
  var searchInput = element(by.id('seach-topology-template-input'));
  searchInput.sendKeys(templateName);
  common.click(by.id('seach-topology-template-btn'));
};
module.exports.searchTopologyTemplate = searchTopologyTemplate;

module.exports.goToTopologyTemplateDetails = function(templateName) {
  go();
  searchTopologyTemplate(templateName);
  common.click(by.id('row_' + templateName));
};

module.exports.checkTopologyTemplate = function(templateName) {
  var elemTemplateId = element(by.id('template_' + templateName + '_name'));
  expect(elemTemplateId.isPresent()).toBe(true);
  expect(elemTemplateId.getText()).toEqual(templateName);
  expect(element(by.id('am.topologytemplate.detail.topology')).isPresent()).toBe(true);
};
