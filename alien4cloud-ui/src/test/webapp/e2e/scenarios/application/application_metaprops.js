/* global by, element, it, describe, expect */
'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');
var xedit = require('../../common/xedit');

var metapropconfiguration = require(__dirname + '/_data/application_metaprops/metaprops.json');

describe('Application meta properties edition check', function() {
  it('beforeAll', function() {
    setup.setup();
    setup.index('metapropconfiguration', 'metapropconfiguration', metapropconfiguration);
    common.home();
    authentication.login('admin');
    applications.goToApplicationDetailPage('AlienUITest');
  });

  it('should display the required class with a specifi class', function() {
    var tagValidValuesName = element(by.id('p_name__ALIEN_RELEASE_VALID_VALUES'));
    expect(tagValidValuesName.getAttribute('class')).toContain('property-required');
  });

  it('should set configuration tags', function() {
    // update _ALIEN_RELEASE_VALID_VALUES
    var metaProperties = element(by.id('meta_properties'));
    var selectItem = metaProperties.element(by.tagName('select'));
    var selected = common.selectDropdownByText(selectItem, 'Q1');
    expect(selected).toBe(true); // Q1 is in the select

    // update _ALIEN_PASSWORD_MIN4
    xedit.sendKeys('div__ALIEN_PASSWORD_MIN4', 'aaaa', false);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
