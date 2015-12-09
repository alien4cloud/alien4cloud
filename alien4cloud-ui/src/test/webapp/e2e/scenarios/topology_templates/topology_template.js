/* global describe, it, element, by, expect */

'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var topologyTemplates = require('../../topology_templates/topology_templates');

var topologyTemplateName = 'MyNewTopologyTemplate';

var checkTopologyTemplate = function(templateName) {
  var elemTemplateId = element(by.id('template_' + templateName + '_name'));
  expect(elemTemplateId.isPresent()).toBe(true);
  expect(elemTemplateId.getText()).toEqual(templateName);
  expect(element(by.id('am.topologytemplate.detail.topology').isPresent())).toBe(true);
};

describe('Topology templates:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('architect');
  });

  it('should be able to add a new topology template', function() {
    topologyTemplates.create('ui test topology template', 'description');
    checkTopologyTemplate(topologyTemplateName);

    topologyTemplates.go();
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('should not be able to add a new topology template with an existing name', function() {
    topologyTemplates.create('ui test topology template', 'description');
    checkTopologyTemplate(topologyTemplateName);

    topologyTemplates.go();
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('should be able to cancel creation of a new topology template', function() {
    topologyTemplates.create('name', 'description', true);
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('should be able to delete a topology template', function() {
    // click remove topology and cancel
    var topologyId = 'template_' + topologyEditorCommon.topologyTemplates.template1.tName;
    var topoTempElement = element(by.id(topologyId));
    common.deleteWithConfirm('delete-' + topologyId, false);
    expect(topoTempElement.isPresent()).toBe(true);

    // click remove topology and confirm
    common.deleteWithConfirm('delete-' + topologyId, true);
    expect(topoTempElement.isPresent()).toBe(false);
  });

  it('should be able to rename a topology template', function() {
  });

  it('should not be able to rename a topology template with an existing name', function() {
  });

  it('should be able to edit the topology template description', function() {
    common.sendValueToXEditable('template_' + newName + '_description', 'New brilliant description', false);
    expect(element(by.binding('topologyTemplate.description')).getText()).toEqual('New brilliant description');
    common.expectNoErrors();
  });

  it('afterAll', function() { authentication.logout(); });
});
