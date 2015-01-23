/* global element, by */

'use strict';

var common = require('../../common/common');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

describe('Editing Relationship name', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to edit a relationship name', function() {
    console.log('################# should be able to edit a relationship name');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);

    var nodeToEdit = element(by.id('rect_JavaRPM'));
    nodeToEdit.click();

    var relashionshipDiv = element(by.id('relationship_hostedOnCompute'));
    var relNameSpan = relashionshipDiv.element(by.css('span[editable-text]'));
    expect(relNameSpan.isDisplayed()).toBe(true);
    var editForm;
    var editInput;
    // success update
    relNameSpan.click();
    editForm = relashionshipDiv.element(by.tagName('form'));
    editInput = editForm.element(by.tagName('input'));
    editInput.clear();
    editInput.sendKeys('hostedOnCompute_renamed');
    editForm.submit();
    browser.waitForAngular();
    relashionshipDiv = element(by.id('relationship_hostedOnCompute_renamed'));
    relNameSpan = relashionshipDiv.element(by.css('span[editable-text]'));
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute_renamed', 1);

    // fail update
    relNameSpan.click();
    editForm = relashionshipDiv.element(by.tagName('form'));
    editInput = editForm.element(by.tagName('input'));
    editInput.clear();
    editInput.sendKeys('dependsOnCompute2');
    editForm.submit();
    browser.waitForAngular();
    topologyEditorCommon.checkCreatedRelationship('hostedOnCompute_renamed', 1);
  });
});
