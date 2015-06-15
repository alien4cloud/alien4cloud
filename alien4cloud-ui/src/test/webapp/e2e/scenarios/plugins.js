/* global element, by */

'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var genericForm = require('../generic_form/generic_form');
var pluginsCommon = require('../admin/plugins_common');
var cloudsCommon = require('../admin/clouds_common');

var pluginId = 'plugin_alien4cloud-mock-paas-provider:1.0';
var pluginName = 'alien4cloud-mock-paas-provider';

describe('Upload and handle paas plugins', function() {

  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    authentication.login('admin');
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    common.after();
  });

  // All tests
  it('should be able to go on plugins list page with ADMIN role', function() {
    pluginsCommon.goToPluginsPage();
  });

  it('should be able to upload plugins mock paas archive from fresh mock jar build', function() {

    // upload mock paas plugin
    pluginsCommon.pluginsUploadInit();

    // check plugin list content
    var results = element.all(by.repeater('plugin in data.data'));
    expect(results.count()).toBeGreaterThan(0);

    results.first().getText().then(function(text) {
      expect(text).toContain(pluginName);
    });

    // plugin id in html : may change if the plugin version change
    var alienMockPaasProviderPlugin = element(by.id(pluginId));
    expect(alienMockPaasProviderPlugin.isPresent()).toBe(true);
  });

  it('should be able to configure uploaded plugin', function() {

    // upload mock paas plugin
    pluginsCommon.pluginsUploadInit();

    var configureButton = browser.element(by.id(pluginId + '_configure'));
    browser.actions().click(configureButton).perform();

    genericForm.sendValueToPrimitive('firstArgument', 'myVeryFirstArgument', false, 'xeditable');

    genericForm.sendValueToPrimitive('secondArgument', 'mySecondArgument', false, 'xeditable');

    genericForm.sendValueToPrimitive('javaVersion', '1.7', false, 'tosca');

    var cpuProperty = {
      'type': 'string',
      'required': true,
      // Problem with scope which became enumeration and not string anymore
      // Generic form do not manage enumeration for the moment
      // 'scope' : 'runtime',
      'default': 4,
      'description': 'Property which define the number of cpu core'
    };

    var cpuPropertyElementType = {
      'type': 'select',
      'required': 'radio',
      'default': 'xeditable',
      'description': 'xeditable'
    };

    genericForm.addKeyToMap('properties', 'cpu', cpuProperty, {}, cpuPropertyElementType);

    var tags = [{
      'name': 'areyougood',
      'value': 'yeahimgood'
    }, {
      'name': 'youwantmore',
      'value': 'yeahsure'
    }];

    var tagsElementType = {
      'name': 'xeditable',
      'value': 'xeditable'
    };

    // Add some instance states
    genericForm.addElementsToArray('tags', tags, false, tagsElementType);

    genericForm.saveForm();

    configureButton = browser.element(by.id(pluginId + '_configure'));
    browser.waitForAngular();
    browser.actions().click(configureButton).perform();
    browser.waitForAngular();
    genericForm.expectValueFromPrimitive('firstArgument', 'myVeryFirstArgument', 'xeditable');
    genericForm.expectValueFromPrimitive('secondArgument', 'mySecondArgument', 'xeditable');
    genericForm.expectValueFromPrimitive('javaVersion', '1.7', 'tosca');
    genericForm.expectValueFromMap('properties', 'cpu', cpuProperty, cpuPropertyElementType);
    genericForm.expectValueFromArray('tags', tags, tagsElementType);
    browser.actions().click(browser.element(by.binding('GENERIC_FORM.CANCEL'))).perform();
    browser.waitForAngular();
  });

  it('shoud be able to drop a plugin when it\'s not used by a another resource', function() {
    console.log('################# shoud be able to drop a plugin when it\'s not used by a another resource');
    // go on plugin list page
    pluginsCommon.goToPluginsPage();

    // upload mock paas plugin
    pluginsCommon.pluginsUploadInit();

    var mockPluginId = 'plugin_alien4cloud-mock-paas-provider:1.0';
    var mockPluginLine = element(by.id(mockPluginId));
    common.deleteWithConfirm('delete-' + mockPluginId, true);
    expect(mockPluginLine.isPresent()).toBe(false);

  });

  it('should be able to cancel the plugin deletion', function() {
    console.log('################# should be able to cancel the plugin deletion');
    pluginsCommon.goToPluginsPage();
    pluginsCommon.pluginsUploadInit();
    var mockPluginId = 'plugin_alien4cloud-mock-paas-provider:1.0';
    var mockPluginLine = element(by.id(mockPluginId));
    common.deleteWithConfirm('delete-' + mockPluginId, false);
    expect(mockPluginLine.isPresent()).toBe(true);
  });


  it('should have an error when trying to activate a cloud without a good configuration', function() {
    console.log('################# should have an error when trying to activate a cloud without a good configuration');
    pluginsCommon.pluginsUploadInit();
    cloudsCommon.goToCloudList();
    cloudsCommon.createNewCloud('testcloud');
    cloudsCommon.goToCloudDetail('testcloud');

    // set bad config to true, deploy it and recive error
    cloudsCommon.goToCloudConfiguration();
    var badConfigurationSwitch = browser.element(by.id('primitiveTypeFormLabelwithBadConfiguratontrue'));
    browser.actions().click(badConfigurationSwitch).perform();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();
    common.expectErrors();

    // set bad config to false, deploy whit success
    cloudsCommon.goToCloudConfiguration();
    badConfigurationSwitch = browser.element(by.id('primitiveTypeFormLabelwithBadConfiguratonfalse'));
    browser.actions().click(badConfigurationSwitch).perform();
    cloudsCommon.goToCloudDetail('testcloud');
    cloudsCommon.enableCloud();
    common.expectNoErrors();
  });

});
