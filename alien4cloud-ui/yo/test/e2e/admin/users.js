/* global element, by */

'use strict';

var common = require('../common/common');
var navigation = require('../common/navigation');

function navigationUsers() {
  navigation.go('main', 'admin');
  navigation.go('admin', 'users');
}
module.exports.navigationUsers = navigationUsers;

function navigationGroups() {
  navigation.go('main', 'admin');
  navigation.go('admin', 'users');
  element(by.id('groups-tab')).element(by.tagName('a')).click();
  browser.waitForAngular();
}
module.exports.navigationGroups = navigationGroups;

module.exports.groups = {
  architects: {
    name: 'Architects',
    email: 'architects@middleearth.nz'
  },
  managers: {
    name: 'Managers',
    email: 'managers@middleearth.nz'
  },
  mordor: {
    name: 'mordor',
    email: 'mordor@middleearth.nz'
  },
  shire: {
    name: 'shire',
    email: 'shire@middleearth.nz'
  }
};

module.exports.createUser = function(userInfos) {
  navigationUsers();

  // Can add a new user
  var btnNewUser = browser.element(by.binding('USERS.NEW'));
  browser.actions().click(btnNewUser).perform();

  element(by.model('user.username')).sendKeys(userInfos.username);
  element(by.model('user.password')).sendKeys(userInfos.password);
  element(by.model('confirmPwd')).sendKeys(userInfos.password);
  if (userInfos.firstName) {
    element(by.model('user.firstName')).sendKeys(userInfos.firstName);
  }
  if (userInfos.lastName) {
    element(by.model('user.lastName')).sendKeys(userInfos.lastName);
  }
  if (userInfos.email) {
    element(by.model('user.email')).sendKeys(userInfos.email);
  }

  // Create a user
  var btnCreate = browser.element(by.binding('CREATE'));
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();
};

module.exports.createGroup = function(groupInfos) {
  navigationGroups();

  // Can add a new group
  var btnNewGroup = browser.element(by.binding('GROUPS.NEW'));
  browser.actions().click(btnNewGroup).perform();

  element(by.model('group.name')).sendKeys(groupInfos.name);
  if (groupInfos.email) {
    element(by.model('group.email')).sendKeys(groupInfos.email);
  }
  if (groupInfos.desc) {
    element(by.model('group.description')).sendKeys(groupInfos.desc);
  }
  // Create a group
  var btnCreate = browser.element(by.binding('CREATE'));
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();
};

module.exports.deleteGroup = function(groupName) {
  navigationGroups();

  common.deleteWithConfirm('group_' + groupName + '_delete', true);
};

module.exports.deleteUser = function(userName) {
  navigationUsers();

  common.deleteWithConfirm('user_' + userName + '_delete', true);
};
