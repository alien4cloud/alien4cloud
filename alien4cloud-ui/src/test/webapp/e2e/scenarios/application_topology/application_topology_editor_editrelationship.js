/* global element, by, it, describe  */
'use strict';

var common = require('../../common/common');
var componentData = require('../../topology/component_data');
var setup = require('../../common/setup');
var csars = require('../../components/csars');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var applications = require('../../applications/applications');
var relCsarPath = require('path').resolve(__dirname, '_data/application_topology_editor_editrelationship/csars/relationshipWithProperties/types.yml.zip');
var toaster = require('../../common/toaster');
var xEdit = require('../../common/xedit');

describe('Editing Relationship name', function() {

  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('applicationManager');
    csars.upload(relCsarPath);
  });

  var testRelationshipTypes = {
    get: function(node, selectedVersion) {
      node.archiveVersion = '1.0.0-SNAPSHOT';
      node.selectedVersion = selectedVersion;
      return node;
    },
    needHelp: function(requestedVersion) {
      return this.get({
        type: 'alien.nodes.NeedHelp',
        id: 'rect_NeedHelp'
      }, requestedVersion);
    },
    hasHelp: function(requestedVersion) {
      return this.get({
        type: 'alien.nodes.HasHelp',
        id: 'rect_HasHelp'
      }, requestedVersion);
    }
  };

  var testRelationshipTopology = {
    nodes: {
      needHelp: testRelationshipTypes.needHelp(),
      hasHelp: testRelationshipTypes.hasHelp()
    },
    relationships: {
      needHelpHostedOnCompute: {
        name: 'hostedOnCompute',
        source: 'NeedHelp',
        requirement: 'host',
        target: 'Compute',
        type: 'tosca.relationships.HostedOn:' + componentData.normativeTypesVersion
      },
      hasHelpHostedOnCompute: {
        name: 'hostedOnCompute',
        source: 'HasHelp',
        requirement: 'host',
        target: 'Compute',
        type: 'tosca.relationships.HostedOn:' + componentData.normativeTypesVersion
      },
      needHelpHelpedByHasHelp: {
        name: 'beHelped',
        source: 'NeedHelp',
        requirement: 'help',
        target: 'HasHelp',
        type: 'alien.relationships.HelpedByTheOneWhoCanHelp:1.0.0-SNAPSHOT'
      }
    }
  };


  it('should be able to edit a relationship name', function() {
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(testRelationshipTopology.nodes);
    topologyEditorCommon.addRelationship(testRelationshipTopology.relationships.needHelpHostedOnCompute);
    topologyEditorCommon.addRelationship(testRelationshipTopology.relationships.hasHelpHostedOnCompute);
    topologyEditorCommon.addRelationship(testRelationshipTopology.relationships.needHelpHelpedByHasHelp);


    // display only one bloc in node details : relationships
    topologyEditorCommon.selectNodeAndGoToDetailBloc('NeedHelp', topologyEditorCommon.nodeDetailsBlocsIds.rel);

    xEdit.sendKeys('relationship_beHelped', 'beHelped_renamed');
    xEdit.expect('relationship_beHelped_renamed', 'beHelped_renamed');
    topologyEditorCommon.checkCreatedRelationship('beHelped_renamed', 1);

    // fail update
    xEdit.sendKeys('relationship_beHelped_renamed', 'hostedOnCompute');
    toaster.expectErrors();
    toaster.dismissIfPresent();

    // Verify everything is saved as expected
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.selectNodeAndGoToDetailBloc('NeedHelp', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    xEdit.expect('relationship_beHelped_renamed', 'beHelped_renamed');
    topologyEditorCommon.checkCreatedRelationship('beHelped_renamed', 1);
  });

  it('should be able to update a property of a relationship', function() {
    xEdit.sendKeys('p_task_name', 'Do my job');
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.selectNodeAndGoToDetailBloc('NeedHelp', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    xEdit.expect('p_task_name', 'Do my job');
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
