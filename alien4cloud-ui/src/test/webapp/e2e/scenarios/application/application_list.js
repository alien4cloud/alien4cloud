/* global describe, it, by, element, expect */
'use strict';

var setup = require('../../common/setup');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');

describe('Applications management list:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
  });

  // TODO: check the pagination and count the app
  it('Admin should be able to see all applications and the create button and check pagination', function() {
    authentication.login('admin');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(true);
  });

  it('Architect should be able to see see it\'s own application and not the create button', function() {
    authentication.logout();
    authentication.login('architect');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(false);
  });

  it('User should be able to see see it\'s own application and not the create button', function() {
    authentication.logout();
    authentication.login('user');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(false);
  });

  it('Application manager should be able to see it\'s own application and the create button', function() {
    authentication.logout();
    authentication.login('applicationManager');
    applications.go();
    expect(element(by.id('app-new-btn')).isPresent()).toBe(true);
  });

  it('Application manager should be able to add a new application with unique name', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    toaster.expectNoErrors();
  });

  it('Application manager should not be able to add a new topology template with existing name', function() {
    applications.createApplication('AlienUITest', 'Great Application');
    toaster.expectErrors();
  });

  it('afterAll', function() { authentication.logout(); });
});
