/* global element, by */

'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var applications = require('../../applications/applications');

describe('NodeTemplate relationships/capability edition', function() {

  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('applicationManager');
    applications.goToApplicationTopologyPage();
  });

  it('should be able display the capability properties', function() {
    console.log('################# should be able display the capability properties');
    var nodeToEdit = element(by.id('rect_Compute'));
    nodeToEdit.click();
    topologyEditorCommon.checkNumberOfPropertiesForACapability('scalable', 3);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
