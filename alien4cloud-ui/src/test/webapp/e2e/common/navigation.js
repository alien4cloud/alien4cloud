/* global by */

'use strict';

var navigationIds = {
  main: {
    applications: 'menu.applications',
    topologyTemplates: 'menu.topologytemplates',
    components: 'menu.components',
    admin: 'menu.admin'
  },
  admin: {
    users: 'am.admin.users',
    plugins: 'am.admin.plugins',
    meta: 'am.admin.metaprops',
    clouds: 'am.admin.clouds.list',
    'cloud-images': 'am.admin.cloud-images.list'
  },
  applications: {
    info: 'am.applications.info',
    topology: 'am.applications.detail.topology',
    plan: 'am.applications.detail.plans',
    deployment: 'am.applications.detail.deployment',
    runtime: 'am.applications.detail.runtime',
    users: 'am.applications.detail.users',
    versions: 'am.applications.detail.versions',
    environments: 'am.applications.detail.environments'
  },
  components: {
    components: 'cm.components.list',
    csars: 'cm.components.csars'
  }
};

module.exports.home = function() {
  browser.get('#/');
  browser.waitForAngular();
};

module.exports.go = function(menu, menuItem) {
  browser.element(by.id(navigationIds[menu][menuItem])).click();
  browser.waitForAngular();
};

module.exports.isPresentButDisabled = function(menu, menuItem) {
  var menuItem = element(by.id(navigationIds[menu][menuItem]));
  expect(menuItem.isDisplayed()).toBe(true);
  expect(menuItem.getAttribute('class')).toContain('disabled');
};

module.exports.isNavigable = function(menu, menuItem) {
  var menuItem = element(by.id(navigationIds[menu][menuItem]));
  expect(menuItem.isDisplayed()).toBe(true);
  expect(menuItem.getAttribute('class')).not.toContain('disabled');
};

module.exports.isNotNavigable = function(menu, menuItem) {
  var menuItem = element(by.id(navigationIds[menu][menuItem]));
  expect(menuItem.isPresent()).toBe(false);
};
