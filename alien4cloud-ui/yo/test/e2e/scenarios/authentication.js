/* global by */

'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');

describe('Authentication tests:', function() {

  beforeEach(function() {
    common.before();
  });

  it('should be able to authenticate as user', function() {
    expect(browser.isElementPresent(by.id('menu.applications'))).toBe(false, 'The application menu should NOT be displayed to users that fails to log in.');
    console.log('################# should be able to authenticate as user');
    // Setting the model
    authentication.login('user');

    // expect to have applications element
    expect(browser.isElementPresent(by.id('menu.applications'))).toBe(true, 'The application menu should be displayed to logged in user.');
  });

  it('should be able to authenticate as admin', function() {
    console.log('################# should be able to authenticate as admin');
    // Setting user login / password model
    authentication.login('admin');

    // expect to have admin element
    expect(browser.isElementPresent(by.id('menu.admin'))).toBe(true, 'The admin menu should be displayed to logged in admin.');
  });

  it('should not be able to authenticate with a bad user', function() {
    console.log('################# should not be able to authenticate with a bad user');
    // Setting user login / password model
    authentication.login('badUser');
    expect(browser.isElementPresent(by.id('menu.applications'))).toBe(false, 'The application menu should NOT be displayed to users that fails to log in.');
    common.expectTitleMessage('401');
    common.expectMessageContent(common.frLanguage.ERRORS['101']);
    common.dismissAlert();
    authentication.login('admin');
  });

  afterEach(function() {
    common.after();
  });
});
