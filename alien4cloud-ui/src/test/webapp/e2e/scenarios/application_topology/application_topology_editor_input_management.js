/* global describe,it */
'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var topologiesData = require(__dirname + '/_data/application_topology_editor_input_managements/topologies.json');
var applications = require('../../applications/applications');

describe('Topology input management', function() {

  it('beforeAll', function() {
    setup.setup();
    setup.index('topology', 'topology', topologiesData);
    common.home();
    authentication.login('applicationManager');
  });

  it('should be able to define properties as input', function() {
    applications.goToApplicationTopologyPage();
    topologyEditorCommon.checkCountInputs(0);
    topologyEditorCommon.togglePropertyInput('Compute', 'architecture', 'cap');
    topologyEditorCommon.checkCountInputs(1);
    topologyEditorCommon.togglePropertyInput('Compute', 'distribution', 'cap');
    topologyEditorCommon.checkCountInputs(2);
  });

  it('should be able to remove an input', function() {
    topologyEditorCommon.removeInput('architecture');
    topologyEditorCommon.checkCountInputs(1);
  });

  it('should be able associate a property to an already existing input', function() {
    topologyEditorCommon.associatePropertyToInput('Compute_2', 'distribution', 'distribution', 'cap');
    topologyEditorCommon.checkCountInputs(1);
  });

  //TODO: Check value of inputs, rename...

  it('afterAll', function() {
    authentication.logout();
  });
});
