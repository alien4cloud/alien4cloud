/* global element, by */

'use strict';

var common = require('../../common/common');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

var javaPuppet = 'fastconnect.nodes.JavaPuppet';
var javaRPM = 'fastconnect.nodes.JavaRPM';

describe('Replacing a node template', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to replace a nodetemplate being a source / target of a relationship', function() {
    console.log('################# should be able to replace a nodetemplate being a source / target of a relationship');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleAbstractTopology.nodes);
    topologyEditorCommon.addRelationship(componentData.simpleAbstractTopology.relationships.hostedOnCompute);
    common.removeAllFacetFilters();

    // replace the Java node
    topologyEditorCommon.replaceNodeTemplates('Java', javaPuppet);
    topologyEditorCommon.checkNodeWasReplaced('Java', 'JavaPuppet');
    // check on relationships count
    topologyEditorCommon.checkNumberOfRelationshipForANode('rect_JavaPuppet', 1);

    // add a relationship with Compute_2, replace JavaPuppet and check
    topologyEditorCommon.addRelationship({
      name: 'dependsOnJava',
      source: 'Compute_2',
      requirement: 'dependency',
      target: 'JavaPuppet',
      type: 'tosca.relationships.DependsOn:2.0'
    });
    topologyEditorCommon.replaceNodeTemplates('JavaPuppet', javaRPM);
    topologyEditorCommon.checkNodeWasReplaced('JavaPuppet', 'JavaRPM');
    topologyEditorCommon.checkNumberOfRelationshipForANode('rect_JavaRPM', 1);

    // check if the target name have been changed in the source of the relationship (Compute_2)
    element(by.id('rect_Compute_2')).click();
    browser.waitForAngular();
    var relationships = element.all(by.repeater('(relationshipName, relationshipDefinition) in selectedNodeTemplate.relationships'));
    browser.waitForAngular();
    expect(relationships.first().element(by.binding('relationshipDefinition.target')).getText()).toContain('JavaRPM');
  });
});
