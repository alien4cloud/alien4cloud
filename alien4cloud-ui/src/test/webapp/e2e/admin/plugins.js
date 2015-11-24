/* global element, by */
'use strict';

var common = require('../common/common');
var settings = require('../common/settings');
var path = require('path');

// Plugins related details paths
var pathToMockPaasPlugin = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-1.0-' + settings.version.version + '.zip');

// Utils to upload plugin archive
var uploadTestMockPaasPlugin = function() {
  common.uploadFile(pathToMockPaasPlugin);
};
module.exports.uploadTestMockPaasPlugin = uploadTestMockPaasPlugin;

// jump to plugins page
var go = function() {
  common.click(by.id('menu.admin'));
  common.click(by.id('am.admin.plugins'));
};
module.exports.go = go;

var pluginsUploadInit = function() {
  go();
  uploadTestMockPaasPlugin();
  common.dismissAlertIfPresent();
};
module.exports.pluginsUploadInit = pluginsUploadInit;

var selectMockPaasProvider = function(paasProviderIndex) {
  // Plugin to select
  if (typeof paasProviderIndex !== 'undefined') {
    // paasProviderIndex should start at 2 since the one at 1 is (no plugin) first ins the list
    var select = element(by.css('select option:nth-child(' + paasProviderIndex + ')'));
    select.click();
  }
};
module.exports.selectMockPaasProvider = selectMockPaasProvider;
