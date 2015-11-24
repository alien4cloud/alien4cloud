/* global by, element, describe, it, expect  */

'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var authentication = require('../../authentication/authentication');
var rolesCommon = require('../../common/roles_common');
var users = require('../../admin/users');

function assertUserExists(username, exists) {
  expect(element(by.id('user_' + username)).isPresent()).toBe(exists);
  if (exists) {
    expect(element(by.id('user_' + username)).element(by.binding('user.username')).getText()).toEqual(username);
  }
}

function assertUserHasNoRoles(username) {
  expect(element(by.id('user_' + username)).isElementPresent(by.css('ul.td_list'))).toBe(false);
}

describe('User management :', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to go to users search page', function() {
    common.log('################# should be able to go to users search page ');
    //go to the users search page
    users.go();
    // the user list table
    expect(element(by.id('users-table')).isPresent()).toBe(true, 'The user table should should be present when we reach the user list page.');
  });

  it('should be able to create/delete a new user, and find it in the users list', function() {
    console.log('################# should be able to create a new user, and find it in the users list');
    //go to the users search page
    users.go();
    //check new user button
    expect(element(by.binding('USERS.NEW')).isPresent()).toBe(true);

    //create and check a user
    users.createUser(authentication.users.sauron);
    assertUserExists(authentication.users.sauron.username, true);

    users.deleteUser(authentication.users.sauron.username);
    assertUserExists(authentication.users.sauron.username, false);
  });

  it('should be able to see and edit user\'s roles', function() {
    console.log('################# should be able to see and edit user\'s roles');
    //create a user and check no roles for now
    users.createUser(authentication.users.bilbo);
    assertUserHasNoRoles(authentication.users.bilbo.username);

    //add roles to bilbo
    rolesCommon.editUserRole(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.editUserRole(authentication.users.bilbo.username, rolesCommon.alienRoles.componentsBrowser);
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.componentsBrowser);

    //refresh the page and check again
    users.go();
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.assertUserHasRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.componentsBrowser);

    //remove roles from bilbo
    rolesCommon.editUserRole(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    rolesCommon.assertUserDoesNotHaveRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
    users.go();
    rolesCommon.assertUserDoesNotHaveRoles(authentication.users.bilbo.username, rolesCommon.alienRoles.admin);
  });

  it('should be able to edit user\'s properties and fields', function() {
    console.log('################# should be able to edit user\'s properties and fields');
    // create a user
    users.createUser(authentication.users.sauron);

    // edit the first name field
    var firstName = 'Sauron';
    xedit.sendKeys('user_' + authentication.users.sauron.username + '_firstName', firstName);
    xedit.expect('user_' + authentication.users.sauron.username + '_firstName', firstName);

    // edit the last name field
    var lastName = 'the Maia';
    xedit.sendKeys('user_' + authentication.users.sauron.username + '_lastName', lastName);
    xedit.expect('user_' + authentication.users.sauron.username + '_lastName', lastName);

    //edit the email field
    var newEmail = 'sauron@mordor.nz';
    xedit.sendKeys('user_' + authentication.users.sauron.username + '_email', newEmail);
    users.go(); // try once with page refresh
    xedit.expect('user_' + authentication.users.sauron.username + '_email', newEmail);
  });

  it('afterAll', function() { authentication.logout(); });
});
