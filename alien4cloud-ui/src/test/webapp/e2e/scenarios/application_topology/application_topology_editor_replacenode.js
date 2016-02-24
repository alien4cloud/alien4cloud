/* global element, by */

'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var applications = require('../../applications/applications');
var topologiesData = require(__dirname + '/_data/application_topology_editor_replacenode/topologies.json');

describe('Replacing a node template', function() {

  it('beforeAll', function() {
    setup.setup();
    setup.index('topology', 'topology', topologiesData);
    common.home();
    authentication.login('applicationManager');
  });

  var checkRelationshipTarget = function(nodeName, targetName) {
    // check if the target name have been changed in the source of the relationship
    topologyEditorCommon.selectNodeAndGoToDetailBloc(nodeName, topologyEditorCommon.nodeDetailsBlocsIds.rel);
    var relationships = element.all(by.repeater('relationshipEntry in selectedNodeTemplate.relationships'));
    expect(relationships.first().element(by.binding('relationshipEntry.value.target')).getText()).toContain(targetName);
  };

  it('should be able to replace a node template being a source / target of a relationship', function() {
    applications.goToApplicationTopologyPage();

    topologyEditorCommon.selectNodeAndGoToDetailBloc('WebApplication', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    checkRelationshipTarget('WebApplication', 'WebServer');

    topologyEditorCommon.selectNodeAndGoToDetailBloc('WebServer', topologyEditorCommon.nodeDetailsBlocsIds.rel);
    checkRelationshipTarget('WebServer', 'Compute');

    topologyEditorCommon.replaceNodeTemplates('WebApplication', 'alien.nodes.Nodecellar');
    topologyEditorCommon.checkNodeWasReplaced('WebApplication', 'Nodecellar');
    topologyEditorCommon.checkNumberOfRelationshipForANode('WebServer', 1);

    checkRelationshipTarget('Nodecellar', 'WebServer');
    topologyEditorCommon.replaceNodeTemplates('WebServer', 'alien.nodes.Nodejs');
    topologyEditorCommon.checkNodeWasReplaced('WebServer', 'Nodejs');
    topologyEditorCommon.checkNumberOfRelationshipForANode('Nodejs', 1);
    checkRelationshipTarget('Nodecellar', 'Nodejs');
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
