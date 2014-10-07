'use strict';

var common = require('./common');
var SCREENSHOT = require('./screenshot');

xdescribe('COMPONENTS : upload components page screenshots', function() {

  beforeEach(function() {
    common.before();
    // Login as component manager
    common.login(common.users.componentManager.username, common.users.componentManager.password);
    // Right dropdown menu
    var navBarRightDrop = browser.element(by.id('navbar-rightdrop'));
    browser.actions().click(navBarRightDrop).perform();
    browser.waitForAngular();
    var linkDropdownEn = browser.element(by.name('link-language-us'));
    linkDropdownEn.click();

  });

  afterEach(function() {
    common.after();
  });

  it("Add components in ALIEN", function() {

    // go to upload page
    common.goToComponentsSearchPage();
    SCREENSHOT.takeScreenShot('upload-page');

    // Upload tosca-base-type and see the result
    common.uploadTestComponents();
    SCREENSHOT.takeScreenShot('upload-components-CM');

  });

});

describe('APPLICATIONS : create a new application in ALIEN', function() {

  beforeEach(function() {
    common.before();
    // Login as application manager
    common.login(common.users.applicationManager.username, common.users.applicationManager.password);
    // Right dropdown menu
    var navBarRightDrop = browser.element(by.id('navbar-rightdrop'));
    browser.actions().click(navBarRightDrop).perform();
    browser.waitForAngular();
    var linkDropdownEn = browser.element(by.name('link-language-us'));
    linkDropdownEn.click();

  });

  afterEach(function() {
    common.after();
  });

  it("Add components in ALIEN", function() {

    // // go to upload page
    common.goToApplicationSearchPage();
    SCREENSHOT.takeScreenShot('applications-page');

    common.createApplication('AlienApplication', 'My great java application running on Tomcat Server.');

  });

});
