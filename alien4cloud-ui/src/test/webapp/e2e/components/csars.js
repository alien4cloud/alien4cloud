/* global by */
'use strict';

var common = require('../common/common');
var components = require('./components');
var toaster = require('../common/toaster');

var go = function() {
  components.go();
  common.click(by.id('cm.components.csars.list'));
};
module.exports.go = go;

var search = function(text) {
  common.sendKeys(by.id('csar-query-input'), text);
  common.click(by.id('btn-search-csar'));
};
module.exports.search = search;

module.exports.open = function(name, version) {
  search(name);
  common.click(by.id('csar_' + name + ':' + version));
};

var git = {
  go: function() {
    components.go();
    common.click(by.id('cm.components.git'));
    browser.actions().mouseMove(element(by.id('btn-new-gitRepository'))).perform();
  },
  search: function(text) {
    common.sendKeys(by.id('search-query'), text);
    common.click(by.id('search-submit-btn'), undefined, undefined, true);
  }
};

module.exports.git = git;

var upload = function(csarPath) {
  components.go();
  common.uploadFile(csarPath);
  toaster.dismissIfPresent();
};

module.exports.upload = upload;
