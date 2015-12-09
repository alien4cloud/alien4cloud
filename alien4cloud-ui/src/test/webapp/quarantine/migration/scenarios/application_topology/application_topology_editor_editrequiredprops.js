/* global element, by */

'use strict';

var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

describe('Editing required properties and checking for topolgy validation', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  // After each spec in the tests suite(s)
  afterEach(function() {
    // Logout action
    authentication.logout();
  });

  it('should be able to see required properties, edit them and make the topology valid', function() {
    console.log('################# should be able to see required properties, edit them and make the topology valid');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom( { compute: componentData.toscaBaseTypes.compute() });
    topologyEditorCommon.checkTodoList(true);

    var node = element(by.id('rect_Compute'));
    node.click();

    topologyEditorCommon.selectNodeAndGoToDetailBloc('Compute', topologyEditorCommon.nodeDetailsBlocsIds.pro);
    expect(element(by.id('p_name_os_arch')).getAttribute('class')).toContain('property-required');
    expect(element(by.id('p_name_os_type')).getAttribute('class')).toContain('property-required');

    //edit the required properties and check again
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    topologyEditorCommon.checkTodoList(false);
  });
});
