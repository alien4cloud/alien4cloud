/* global by, element */
'use strict';

var common = require('../common/common');

var go = function() {
  common.click(by.id('menu.applications'));
};
module.exports.go = go;

var createApplication = function(newAppName, newAppDescription, templateName, templateVersion) {
  go();
  common.click(by.id('app-new-btn'));
  element(by.model('app.name')).sendKeys(newAppName);
  element(by.model('app.description')).sendKeys(newAppDescription);

  // Template to select
  if (typeof templateName !== 'undefined') {

    if (templateName !== '*') {
      common.select(by.id('templateid'), templateName);
    } else {
      // take the first template
      element(by.id('templateid')).element(by.css('select option:nth-child(2)')).click();
    }

    if (typeof templateVersion !== 'undefined') {
      common.select(by.id('templateVersionId'), templateVersion);
    } else {
      // take the first version
      element(by.id('templateVersionId')).element(by.css('select option:nth-child(2)')).click();
    }

  }
  common.click(by.id('btn-create'));
};
module.exports.createApplication = createApplication;

function goToApplicationDetailPage(applicationName) {
  common.go('main', 'applications');
  // From the application search page select a particular line
  if(applicationName) {
    var appElement = element(by.id('app_' + applicationName)); // .click();
    appElement.click();
  } else {
    browser.element(by.binding('application.name')).click();
  }
  browser.waitForAngular();
}

module.exports.goToApplicationDetailPage = goToApplicationDetailPage;

function goToApplicationTopologyPage(applicationName) {
  goToApplicationDetailPage(applicationName);
  common.go('applications', 'topology');
}
module.exports.goToApplicationTopologyPage = goToApplicationTopologyPage;

function goToApplicationDeploymentPage(applicationName) {
  goToApplicationDetailPage(applicationName);
  common.go('applications', 'deployment');
}
module.exports.goToApplicationDeploymentPage = goToApplicationDeploymentPage;

var searchApplication = function(appName) {
  var searchInput = element(by.id('seach-applications-input'));
  searchInput.clear();
  searchInput.sendKeys(appName);
  common.click(by.id('seach-applications-btn'));
};
module.exports.searchApplication = searchApplication;
