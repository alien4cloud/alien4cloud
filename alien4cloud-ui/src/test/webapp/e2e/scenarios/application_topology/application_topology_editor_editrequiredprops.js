/* global element, by */

'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var topologiesData = require(__dirname + '/_data/application_topology_editor_editrequiredprops/topologies.json');
var deploymentTopologiesData = require(__dirname + '/_data/application_topology_editor_editrequiredprops/deploymenttopologies.json');
var applications = require('../../applications/applications');

describe('Editing required properties and checking for topology validation', function() {

  it('beforeAll', function() {
    setup.setup();
    setup.index('topology', 'topology', topologiesData);
    setup.index('deploymenttopology', 'deploymenttopology', deploymentTopologiesData);
    common.home();
    authentication.login('applicationManager');
  });

  it('should be able to see required properties, edit them and make the topology valid', function() {
    console.log('################# should be able to see required properties, edit them and make the topology valid');
    applications.goToApplicationTopologyPage();
    // Property context_path of the node war is not present
    topologyEditorCommon.checkTodoList(true);
    // Set the property
    topologyEditorCommon.selectNodeAndGoToDetailBloc('War', topologyEditorCommon.nodeDetailsBlocsIds.pro);
    topologyEditorCommon.editNodeProperty('War', 'context_path', '/');
    expect(element(by.id('div_context_path')).getAttribute('class')).toContain('property-required');
    // Now don't have anymore error
    topologyEditorCommon.checkTodoList(false);
    // Delete the required properties and check again
    topologyEditorCommon.editNodeProperty('War', 'context_path', '');
    topologyEditorCommon.checkTodoList(true);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
