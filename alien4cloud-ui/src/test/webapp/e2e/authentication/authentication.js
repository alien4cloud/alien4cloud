/**
 * Module that exposes functions to login - logout and re-login with various users.
 */
/* global by, element */

var common = require('../common/common');

'use strict';

//users
var users = {
  admin: {
    username: 'admin',
    password: 'admin',
    registered: true
  },
  componentManager: {
    username: 'componentManager',
    password: 'componentManager',
    registered: true
  },
  componentBrowser: {
    username: 'componentBrowser',
    password: 'componentBrowser',
    registered: true
  },
  architect: {
    username: 'architect',
    password: 'architect',
    registered: true
  },
  applicationManager: {
    username: 'applicationManager',
    password: 'applicationManager',
    registered: true
  },
  user: {
    username: 'user',
    password: 'user',
    registered: true
  },
  badUser: {
    username: 'toto',
    password: 'tata',
    registered: false
  },
  sauron: {
    username: 'sauron',
    password: 'sauron',
    firstName: 'Mairon',
    lastName: 'L\'admirable',
    email: 'mairon@aule.forge.nz',
    registered: false
  },
  bilbo: {
    username: 'bilbo',
    password: 'bilbo',
    firstName: 'Bilbo',
    lastName: 'Baggins',
    email: 'bilbo@baggins.nz',
    registered: false
  }
};

module.exports.users = users;

function logout() {
  common.dismissAlertIfPresent();
  common.click(by.id('navbar-rightdrop'));
  // skip the wait for angular after a logout operation as there is a redirect operation on the browser.
  common.click(by.name('btn-logout'), null, true);
}

function login(username) {
  var user = users[username];
  // Setting the model
  var userInput = common.waitElement(by.model('login.username'));
  userInput.clear();
  userInput.sendKeys(user.username);
  var pwdInput = common.waitElement(by.model('login.password'));
  pwdInput.clear();
  pwdInput.sendKeys(user.password);

  // Login click action
  common.click(by.name('btn-login'));
}

function reLogin(username) {
  logout();
  login(username);
}

module.exports.login = login;
module.exports.logout = logout;
module.exports.reLogin = reLogin;
