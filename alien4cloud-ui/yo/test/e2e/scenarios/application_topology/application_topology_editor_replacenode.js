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
    topologyEditorCommon.checkNumberOfRelationshipForANode('JavaPuppet', 1);

    // add a relationship with Compute_2, replace JavaPuppet and check
    topologyEditorCommon.addRelationshipToNode('Compute_2', 'JavaPuppet', 'dependency', 'tosca.relationships.DependsOn:2.0', 'dependsOnJavaPuppet');

    topologyEditorCommon.replaceNodeTemplates('JavaPuppet', javaRPM);
    topologyEditorCommon.checkNodeWasReplaced('JavaPuppet', 'JavaRPM');
    topologyEditorCommon.checkNumberOfRelationshipForANode('JavaRPM', 1);

    // check if the target name have been changed in the source of the relationship (Compute_2)
    topologyEditorCommon.selectNodeAndGoToDetailBloc('Compute_2', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    var relationships = element.all(by.repeater('relationshipEntry in selectedNodeTemplate.relationships'));
    expect(relationships.first().element(by.binding('relationshipEntry.value.target')).getText()).toContain('JavaRPM');
  });
});
