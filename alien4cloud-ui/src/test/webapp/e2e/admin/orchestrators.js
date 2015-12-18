/* global by, expect */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.admin'));
  common.click(by.id('am.admin.orchestrators'));
};
module.exports.go = go;

module.exports.create = function(name, cancel) {
  go();
  common.click(by.id('new-orchestrator'));
  // assert that the create button is disabled when there is no name.
  var createButton = common.element(by.id('modal-create-button'));
  expect(createButton.getAttribute('disabled')).toEqual('true');
  common.sendKeys(by.id('orchestrator_name_id'), name);
  expect(createButton.getAttribute('disabled')).toEqual('true');
  common.select(by.model('newOrchestrator.plugin'), 'Mock Orchestrator Factory : 1.0');
  if(cancel) {
    common.click(by.id('modal-cancel-button'));
  } else {
    common.click(by.id('modal-create-button'));
  }
};
