/* global by */

'use strict';

var common = require('../common/common');

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
  },
  allusers: {
    name: 'ALL_USERS',
    email: 'all@alien4cloud.fr',
    description: 'Default alien internal group'
  }
};

function go() {
  common.click(by.id('menu.admin'));
  common.click(by.id('am.admin.users'));
}
module.exports.go = go;

function goToGroups() {
  go();
  var groupTab = common.element(by.id('groups-tab'));
  common.click(by.tagName('a'), groupTab);
}
module.exports.goToGroups = goToGroups;

module.exports.createUser = function(userInfos) {
  go();
  // Open the creation modal
  common.click(by.binding('USERS.NEW'));

  // Fill form data
  common.sendKeys(by.model('user.username'), userInfos.username);
  common.sendKeys(by.model('user.password'), userInfos.password);
  common.sendKeys(by.model('confirmPwd'), userInfos.password);
  if (userInfos.firstName) {
    common.sendKeys(by.model('user.firstName'), userInfos.firstName);
  }
  if (userInfos.lastName) {
    common.sendKeys(by.model('user.lastName'), userInfos.lastName);
  }
  if (userInfos.email) {
    common.sendKeys(by.model('user.email'), userInfos.email);
  }

  // Create
  common.click(by.binding('CREATE'));
};

module.exports.deleteUser = function(userName) {
  go();
  common.deleteWithConfirm('user_' + userName + '_delete', true);
};

module.exports.createGroup = function(groupInfos) {
  goToGroups();

  // Open the creation modal
  common.click(by.binding('GROUPS.NEW'));

  // Fill form data
  common.sendKeys(by.model('group.name'), groupInfos.name);
  if (groupInfos.email) {
    common.sendKeys(by.model('group.email'), groupInfos.email);
  }
  if (groupInfos.desc) {
    common.sendKeys(by.model('group.description'), groupInfos.desc);
  }
  // Create
  common.click(by.binding('CREATE'));
};

module.exports.deleteGroup = function(groupName) {
  goToGroups();
  common.deleteWithConfirm('group_' + groupName + '_delete', true);
};
