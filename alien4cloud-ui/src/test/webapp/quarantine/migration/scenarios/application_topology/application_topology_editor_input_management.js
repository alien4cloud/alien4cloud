/* global by,element */

'use strict';

var authentication = require('../../authentication/authentication');
var applications = require('../../applications/applications');
var common = require('../../common/common');
var navigation = require('../../common/navigation');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

describe('Topology input/output properties', function() {
  var reset = true;
  var after = false;

  beforeEach(function() {
    if (reset) {
      reset = false;
      common.before();
      authentication.login('applicationManager');
      applications.createApplication('Alien', 'Great Application');
      browser.element(by.binding('application.name')).click();
      navigation.go('main', 'applications');
      browser.element(by.binding('application.name')).click();
      navigation.go('applications', 'topology');
      topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
    }
  });

  afterEach(function() {
    if (after) {
      authentication.logout();
    }
  });

  it('should be able to define properties as input', function() {
    console.log('################# should be able to define properties as input');
    topologyEditorCommon.checkCountInputs(0);
    topologyEditorCommon.togglePropertyInput('Compute', 'ip_address');
    topologyEditorCommon.checkCountInputs(1);
    topologyEditorCommon.togglePropertyInput('Compute', 'os_arch');
    topologyEditorCommon.checkCountInputs(2);
  });

  it('should be able to remove an input', function() {
    console.log('################# should be able to remove an input.');
    topologyEditorCommon.removeInput('ip_address');
    topologyEditorCommon.checkCountInputs(1);
  });

  it('should be able associate a property to an already existing input', function() {
    after = true;
    console.log('################# should be able associate a property to an already existing input.');
    topologyEditorCommon.associatePropertyToInput('Compute-2', 'os_arch', 'os_arch');
    topologyEditorCommon.checkCountInputs(1);
  });

  //TODO: Check value of inputs, rename...
});
