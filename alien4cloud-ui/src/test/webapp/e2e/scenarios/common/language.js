/* global describe, it, by, element, expect */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');

describe('Language change', function() {
  it('beforeAll', function() { setup.setup(); });

  it('Can select french language', function() {
    console.log('################# Can select french language');
    common.home();
    common.click(by.id('navbar-rightdrop'));
    common.click(by.name('link-language-fr'));
  });

  it('should be able to switch between french and english on the home page.', function() {
    console.log('################# should be able to switch between french and english on the home page.');
    common.home();

    // Select french
    common.click(by.id('navbar-rightdrop'));
    common.click(by.name('link-language-fr'));

    expect(element(by.name('btn-login')).getText()).toContain(common.frLanguage.NAVBAR.BUTTON_LOGIN);
    expect(element(by.binding('MAIN.WELCOME')).getText()).toContain(common.frLanguage.MAIN.WELCOME);

    // Select english
    common.click(by.id('navbar-rightdrop'));
    common.click(by.name('link-language-us'));

    expect(element(by.name('btn-login')).getText()).toContain(common.usLanguage.NAVBAR.BUTTON_LOGIN);
    expect(element(by.binding('MAIN.WELCOME')).getText()).toContain(common.usLanguage.MAIN.WELCOME);
  });
});
