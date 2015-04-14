/* global element, by */

'use strict';
var common = require('../../common/common');
var navigation = require('../../common/navigation');
var applications = require('../../applications/applications');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

var computesNodeTemplates = {
    compute: componentData.toscaBaseTypes.compute(),
    ubuntu: componentData.ubuntuTypes.ubuntu()
};

var confirmAction = function(confirmPopup){
  var confirmBtn = confirmPopup.element(by.css('.btn-success'));
  confirmBtn.click();
  browser.sleep(8000); // Time needed to complete the scaling
};

var cancelAction = function(cancelPopup){
  var cancelBtn = cancelPopup.element(by.css('.btn-danger'));
  cancelBtn.click();
};

var scale = function(oldValue, newValue, cancel) {
  common.slideXEditableTo('scaleEditableInput', newValue);
  browser.waitForAngular();
  var scaleEditableInput = element(by.id('scaleEditableInput'));
  var editForm = scaleEditableInput.element(by.tagName('form'));
  var submitBtn = editForm.element(by.css('button.btn-primary'));
  submitBtn.click();
  var valueToCheck;
  if (cancel) {
    valueToCheck = oldValue;
    cancelAction(element(by.css('.popover')));
  } else {
    valueToCheck = newValue;
    confirmAction(element(by.css('.popover')));
  }
  expect(scaleEditableInput.getText()).toContain(valueToCheck);
};

var checkAndScale = function(nodeId, valueToCheck, newValue, cancel){
  var nodeToView = element(by.id(nodeId));
  nodeToView.click();
  element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).then(function(states) {
    expect(states.length).toEqual(valueToCheck);
    states[0].element(by.css('.btn-default')).click(); // the view instance details button
    expect(element.all(by.repeater('(propKey, propVal) in selectedInstance.runtimeProperties')).count()).toEqual(1);
    var backButton = browser.element(by.id('backToInstanceListButton'));
    browser.actions().click(backButton).perform();
    element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).then(function(states) {
      expect(states.length).toEqual(valueToCheck);
      if (newValue) {
        scale(valueToCheck, newValue, cancel);
      }
    });
  });
};


describe('Topology scaling feature', function() {
  var reset = true;
  var after = false;

  beforeEach(function() {
    if (reset) {
      reset = false;
      topologyEditorCommon.beforeTopologyTest();
    }
  });

  afterEach(function() {
    if (after) {
      common.after();
    }
  });

  it('should be able to add scaling policy', function() {
    console.log('################# should be able to add scaling policy.');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(computesNodeTemplates);

    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
    browser.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.removeScalingPolicy('rect_Compute');
    });
    browser.sleep(1000);
    expect(element(by.id('maxInstances')).isPresent()).toBe(false);
    expect(element(by.id('minInstances')).isPresent()).toBe(false);
    expect(element(by.id('initialInstances')).isPresent()).toBe(false);

    topologyEditorCommon.addScalingPolicy('rect_Ubuntu', 1, 3, 3);
    browser.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.removeScalingPolicy('rect_Ubuntu');
    });
    browser.sleep(1000);
    expect(element(by.id('maxInstances')).isPresent()).toBe(false);
    expect(element(by.id('minInstances')).isPresent()).toBe(false);
    expect(element(by.id('initialInstances')).isPresent()).toBe(false);

    // Change scaling policy
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    topologyEditorCommon.editNodeProperty('Ubuntu', 'containee_types', 'test', 'cap');
    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
    topologyEditorCommon.addScalingPolicy('rect_Ubuntu', 1, 3, 3);
  });

  it('should be able to deploy and scale (with confirmation) a compute, and every node derived from it', function() {
    after = true;
    console.log('################# should be able to deploy and scale (with confirmation) a compute, and every node derived from it.');
    applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);
    navigation.go('applications', 'runtime');

    browser.sleep(10000); // Wait for mock deployment to finish
    checkAndScale('rect_Compute', 2 , 1);
    checkAndScale('rect_Compute', 1);
    checkAndScale('rect_Compute', 1, 2, true);
    checkAndScale('rect_Compute', 1);

    checkAndScale('rect_Ubuntu', 3, 1);
    checkAndScale('rect_Ubuntu', 1);
    checkAndScale('rect_Ubuntu', 1, 2, true);
    checkAndScale('rect_Ubuntu', 1);
  });

});
