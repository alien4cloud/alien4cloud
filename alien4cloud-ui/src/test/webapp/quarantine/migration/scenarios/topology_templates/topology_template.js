/* global element, by */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var topologyTemplatesCommon = require('../../topology/topology_templates_common');

var topologyTemplateName = 'MyNewTopologyTemplate';

var checkTopologyTemplate = function(templateName) {
  var elemTemplateId = element(by.id('template_' + templateName + '_name'));
  expect(elemTemplateId.isPresent()).toBe(true);
  expect(elemTemplateId.getText()).toEqual(templateName);
};

describe('Create topology templates', function() {

  beforeEach(function() {
    common.before();
    authentication.login('admin');
    authentication.reLogin('architect');
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    authentication.logout();
  });

  it('should add a new topology template and be redirected on template topology edition', function() {
    console.log('################# should add a new topology template and be redirected on template topology edition');
    topologyTemplatesCommon.createTopologyTemplate(topologyTemplateName, 'A first basic topology template');

    // Testing URL and id displayed
    checkTopologyTemplate(topologyTemplateName);

    // Jump to Template list for count()
    navigation.go('main', 'topologyTemplates');

    var topologyTemplates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(topologyTemplates.count()).toBe(1);
  });

  it('should add ' + Object.keys(topologyEditorCommon.topologyTemplates).length + ' topology templates, and delete one (cancel then confirm delete)', function() {
    console.log('################# should add ' + Object.keys(topologyEditorCommon.topologyTemplates).length + ' topology templates, and delete one (cancel then confirm delete)');
    // Jump to Template list for count()
    Object.keys(topologyEditorCommon.topologyTemplates).forEach(function(templateKey) {
      topologyTemplatesCommon.createTopologyTemplate(topologyEditorCommon.topologyTemplates[templateKey].tName, topologyEditorCommon.topologyTemplates[templateKey].tDescription);
      checkTopologyTemplate(topologyEditorCommon.topologyTemplates[templateKey].tName);
    });

    navigation.go('main', 'topologyTemplates');
    var topoTemplates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(topoTemplates.count()).toBe(Object.keys(topologyEditorCommon.topologyTemplates).length);

    // click remove topology and cancel
    var topologyId = 'template_' + topologyEditorCommon.topologyTemplates.template1.tName;
    var topoTempElement = element(by.id(topologyId));
    common.deleteWithConfirm('delete-' + topologyId, false);
    expect(topoTempElement.isPresent()).toBe(true);

    // click remove topology and confirm
    common.deleteWithConfirm('delete-' + topologyId, true);
    expect(topoTempElement.isPresent()).toBe(false);
  });

  it('should be able to edit a template', function() {
    console.log('################# should be able to edit a template name');
    var secondTemplateName = 'secondTemplate';
    var newName = 'templateRenamed';
    topologyTemplatesCommon.createTopologyTemplate(topologyTemplateName, 'A first basic topology template');
    checkTopologyTemplate(topologyTemplateName);
    topologyTemplatesCommon.createTopologyTemplate(secondTemplateName, 'A second basic topology template');
    checkTopologyTemplate(secondTemplateName);

    topologyTemplatesCommon.goToTemplateDetailPage(topologyTemplateName);
    common.sendValueToXEditable('template_' + topologyTemplateName + '_name', newName, false);
    checkTopologyTemplate(newName);

    // Change the template's description
    common.sendValueToXEditable('template_' + newName + '_description', 'New brilliant description', false);
    expect(element(by.binding('topologyTemplate.description')).getText()).toEqual('New brilliant description');
    common.expectNoErrors();

    //should fail to rename with an existing name
    topologyTemplatesCommon.goToTemplateDetailPage(secondTemplateName);
    common.sendValueToXEditable('template_' + secondTemplateName + '_name', newName, false);
    common.abortXEditable('template_' + secondTemplateName + '_name');
    common.expectTitleMessage('409');
    common.dismissAlert();
    checkTopologyTemplate(secondTemplateName);
  });
});
