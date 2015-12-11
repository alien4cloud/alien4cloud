/* global by, element */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.applications'));
};
module.exports.go = go;

// Expose create application functions
var createApplication = function(newAppName, newAppDescription, topologyTemplateSelectNumber) {
  go();
  common.click(by.id('app-new-btn'));
  element(by.model('app.name')).sendKeys(newAppName);
  element(by.model('app.description')).sendKeys(newAppDescription);

  // Template to select
  if (typeof topologyTemplateSelectNumber !== 'undefined') {
    // topologyTemplateSelectNumber should start at 2 since the one at 1 is (no template) first ins the list
    var select = element(by.id('templateid')).element(by.css('select option:nth-child(' + topologyTemplateSelectNumber + ')'));
    select.click();

    // take the first version
    select = element(by.id('templateVersionId')).element(by.css('select option:nth-child(2)'));
    select.click();
  }
  common.click(by.id('btn-create'));
};
module.exports.createApplication = createApplication;
