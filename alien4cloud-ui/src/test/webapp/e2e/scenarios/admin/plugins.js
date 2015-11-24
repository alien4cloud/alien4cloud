/* global by, element, describe, it, expect */

'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var genericForm = require('../../generic_form/generic_form');
var plugins = require('../../admin/plugins');

var pluginId = 'plugin_alien4cloud-mock-paas-provider:1.0';
var pluginName = 'alien4cloud-mock-paas-provider';

describe('Upload and handle paas plugins', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to go on plugins list page with ADMIN role', function() {
    plugins.go();
    // TODO we should check here that we are on the right page.
  });

  it('should be able to upload plugins mock paas archive from fresh mock jar build', function() {
    // upload mock paas plugin
    plugins.pluginsUploadInit();

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
    plugins.pluginsUploadInit();

    common.click(by.id(pluginId + '_configure'));

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

    common.click(by.id(pluginId + '_configure'));
    genericForm.expectValueFromPrimitive('firstArgument', 'myVeryFirstArgument', 'xeditable');
    genericForm.expectValueFromPrimitive('secondArgument', 'mySecondArgument', 'xeditable');
    genericForm.expectValueFromPrimitive('javaVersion', '1.7', 'tosca');
    genericForm.expectValueFromMap('properties', 'cpu', cpuProperty, cpuPropertyElementType);
    genericForm.expectValueFromArray('tags', tags, tagsElementType);

    common.click(by.binding('GENERIC_FORM.CANCEL'));
  });

  it('shoud be able to drop a plugin when it\'s not used by a another resource', function() {
    console.log('################# shoud be able to drop a plugin when it\'s not used by a another resource');
    // upload mock paas plugin
    plugins.pluginsUploadInit();

    var mockPluginId = 'plugin_alien4cloud-mock-paas-provider:1.0';
    var mockPluginLine = element(by.id(mockPluginId));
    common.deleteWithConfirm('delete-' + mockPluginId, true);
    expect(mockPluginLine.isPresent()).toBe(false);
  });

  it('should be able to cancel the plugin deletion', function() {
    console.log('################# should be able to cancel the plugin deletion');
    plugins.pluginsUploadInit();
    var mockPluginId = 'plugin_alien4cloud-mock-paas-provider:1.0';
    var mockPluginLine = element(by.id(mockPluginId));
    common.deleteWithConfirm('delete-' + mockPluginId, false);
    expect(mockPluginLine.isPresent()).toBe(true);
  });

  it('afterAll', function() { authentication.logout(); });
});
