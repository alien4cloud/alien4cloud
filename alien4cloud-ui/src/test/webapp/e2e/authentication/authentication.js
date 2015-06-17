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
  common.click(by.name('btn-logout'));
}

function login(username) {
  var user = users[username];
  // Setting the model
  var userInput = element(by.model('login.username'));
  userInput.clear();
  userInput.sendKeys(user.username);
  var pwdInput = element(by.model('login.password'));
  pwdInput.clear();
  pwdInput.sendKeys(user.password);

  // Login click action
  var btnLogin = browser.element(by.name('btn-login'));
  browser.actions().click(btnLogin).perform();
  browser.waitForAngular();
}

function reLogin(username) {
  logout();
  login(username);
}

module.exports.login = login;
module.exports.logout = logout;
module.exports.reLogin = reLogin;
