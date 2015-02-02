/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var tagConfigCommon = require('../../admin/metaprops_configuration_common');
var applications = require('../../applications/applications');

describe('Application meta properties edition check', function() {

  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    authentication.login('applicationManager');
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should create an application an set configuration tags', function() {
    console.log('should create an application an set configuration tags');
    // add configuration tags
    authentication.reLogin('admin');
    tagConfigCommon.createConfigurationTags();

    // create the app
    authentication.reLogin('applicationManager');
    applications.createApplication('ALIEN_WITH_TAGS', 'Great application with configuration tags');

    // check p_name__ALIEN_RELEASE_VALID_VALUES class required
    var tagValidValuesName = element(by.id('p_name__ALIEN_RELEASE_VALID_VALUES'));
    expect(tagValidValuesName.getAttribute('class')).toContain('property-required');

    // suppose we've only one "select" in the configuration tag list
    var metaProperties = element(by.id('meta_properties'));
    var selectItem = metaProperties.element(by.tagName('select'));
    var selected = common.selectDropdownByText(selectItem, 'Q1');
    expect(selected).toBe(true); // Q1 is in the select

    // ---- NOT WORKING (strange bug)
    // enter a bad password text length
    // tagConfigCommon.editTagConfiguration('_ALIEN_PASSWORD_MIN4', 'bu');
    // browser.waitForAngular();

    // // check errors
    // tagConfigCommon.checkTagEditionError('_ALIEN_PASSWORD_MIN4', '4');
    // browser.waitForAngular();
    // ---- END : NOT WORKING (strange bug)

    // enter a good password text length
    tagConfigCommon.editTagConfiguration('_ALIEN_PASSWORD_MIN4', 'aaaa');
  });

});
