/* global element, by */
'use strict';

var navigation = require('../common/navigation');
var common = require('../common/common');
var settings = require('../common/settings');
var path = require('path');

// Plugins related details paths

var pathToMockPaasPlugin = path.resolve(__dirname, '../../../../../../alien4cloud-mock-paas-provider-plugin/target/alien4cloud-mock-paas-provider-plugin-' + settings.version.version + '.zip');

// Utils to upload plugin archive
var uploadTestMockPaasPlugin = function() {
  common.uploadFile(pathToMockPaasPlugin);
};
module.exports.uploadTestMockPaasPlugin = uploadTestMockPaasPlugin;

// jump to plugins page
var goToPluginsPage = function() {
  navigation.go('main', 'admin');
  navigation.go('admin', 'plugins');
};
module.exports.goToPluginsPage = goToPluginsPage;

var pluginsUploadInit = function() {
  goToPluginsPage();
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
