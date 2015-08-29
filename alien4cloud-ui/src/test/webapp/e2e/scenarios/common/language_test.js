/* global by, element */
'use strict';

var common = require('../../common/common');

describe('Language change', function() {

  beforeEach(function() {
    common.before();
  });

  it('need french as default language', function() {
    console.log('################# need french as default language');
    // Right dropdown menu
    var navBarRightDrop = browser.element(by.id('navbar-rightdrop'));
    browser.actions().click(navBarRightDrop).perform();
    browser.waitForAngular();

    // Select french as language
    var linkDropdownFr = browser.element(by.name('link-language-fr'));
    linkDropdownFr.click();
  });

  it('should be able to switch between french and english on the home page.', function() {
    console.log('################# should be able to switch between french and english on the home page.');
    // Select dropdown button
    // Right dropdown menu
    var navBarRightDrop = browser.element(by.id('navbar-rightdrop'));
    browser.actions().click(navBarRightDrop).perform();
    browser.waitForAngular();

    var linkDropdownFr = browser.element(by.name('link-language-fr'));
    var linkDropdownUs = browser.element(by.name('link-language-us'));

    // Select french as language
    browser.actions().click(linkDropdownFr).perform();
    browser.waitForAngular();

    expect(element(by.name('btn-login')).getText()).toContain(common.frLanguage.NAVBAR.BUTTON_LOGIN);
    expect(element(by.binding('MAIN.WELCOME')).getText()).toContain(common.frLanguage.MAIN.WELCOME);

    // Select english
    browser.actions().click(navBarRightDrop).perform();
    browser.waitForAngular();
    linkDropdownUs.click();

    expect(element(by.name('btn-login')).getText()).toContain(common.usLanguage.NAVBAR.BUTTON_LOGIN);
    expect(element(by.binding('MAIN.WELCOME')).getText()).toContain(common.usLanguage.MAIN.WELCOME);
  });
});
