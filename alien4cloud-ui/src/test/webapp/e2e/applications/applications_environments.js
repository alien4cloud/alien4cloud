/* global by, element */
'use strict';

var common = require('../common/common');

var environmentTypes = {
  other: 'OTHER',
  dev: 'DEVELOPMENT',
  it: 'INTEGRATION_TESTS',
  uat: 'USER_ACCEPTANCE_TESTS',
  pprod: 'PRE_PRODUCTION',
  prod: 'PRODUCTION'
};
module.exports.environmentTypes = environmentTypes;

var go = function() {
  common.click(by.id('am.applications.detail.environments'));
};
module.exports.go = go;

var createApplicationEnvironment = function(envName, envDescription, envTypeSelectName, appVersionName) {
  go();
  common.click(by.id('app-env-new-btn'));

  element(by.model('environment.name')).sendKeys(envName);
  element(by.model('environment.description')).sendKeys(envDescription);

  if (typeof envTypeSelectName !== 'undefined') {
    // envTypeSelectNumber should start at 2 since the one at 1 is (no envTypeSelectNumber) first ins the list
    var selectType = element(by.id('envtypelistid'));
    common.selectDropdownByText(selectType, common.usLanguage.CLOUDS.ENVIRONMENT[envTypeSelectName]);
  } else {
    console.error('You should have at least one environment type defined');
  }

  if (typeof appVersionName !== 'undefined') {
    var selectVersion = element(by.id('versionslistid'));
    common.selectDropdownByText(selectVersion, appVersionName);
  } else {
    console.error('You should have at least one application version type defined');
  }

  common.click(by.id('btn-create'));
};
module.exports.createApplicationEnvironment = createApplicationEnvironment;
