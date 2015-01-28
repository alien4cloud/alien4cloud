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
};

var cancelAction = function(confirmPopup){
  var cancelBtn = confirmPopup.element(by.css('.btn-danger'));
  cancelBtn.click();
};

var scale = function(oldValue, newValue, cancel) {
  var scaleEditableInput = element(by.id('scaleEditableInput'));
  var scaleEditableButton = element(by.id('scaleEditableButton'));
  scaleEditableButton.click();
  var editForm = scaleEditableInput.element(by.tagName('form'));
  var editInput = editForm.element(by.tagName('input'));
  var submitBtn = editForm.element(by.css('button.btn-primary'));
  editInput.clear();
  editInput.sendKeys(newValue);
  browser.waitForAngular();
  submitBtn.click();
  var valueToCheck ;
  if(cancel){
    valueToCheck = oldValue;
    cancelAction(element(by.css('.popover')));
  }else{
    valueToCheck = newValue;
    confirmAction(element(by.css('.popover')));
  }
  browser.waitForAngular();
  expect(scaleEditableInput.getText()).toContain(valueToCheck);
};

var checkAndScale = function(nodeId, valueToCheck, newValue, cancel){
  var nodeToView = element(by.id(nodeId));
  nodeToView.click();
  element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).then(function(states) {
    expect(states.length).toEqual(valueToCheck);
    states[0].click();
    expect(element.all(by.repeater('(propKey, propVal) in selectedInstance.runtimeProperties')).count()).toEqual(2);
    var backButton = browser.element(by.id('backToInstanceListButton'));
    browser.actions().click(backButton).perform();
    element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).then(function(states) {
      expect(states.length).toEqual(valueToCheck);
      if(newValue) {
        scale(valueToCheck, newValue, cancel);
      }
    });
  });
};


describe('Topology scaling feature', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  afterEach(function() {
    common.after();
  });

  it('should be able to add scaling policy, deploy and scale (with confirmation)  a compute, and every node derived from it', function() {
    console.log('################# should be able to add scaling policy, deploy and scale (with confirmation) a compute, and every node derived from it');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(computesNodeTemplates);

    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
    common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.removeScalingPolicy('rect_Compute');
    });
    browser.sleep(1000);
    expect(element(by.id('maxInstances')).isPresent()).toBe(false);
    expect(element(by.id('minInstances')).isPresent()).toBe(false);
    expect(element(by.id('initialInstances')).isPresent()).toBe(false);

    topologyEditorCommon.addScalingPolicy('rect_Ubuntu', 1, 3, 3);
    common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.removeScalingPolicy('rect_Ubuntu');
    });
    browser.sleep(1000);
    expect(element(by.id('maxInstances')).isPresent()).toBe(false);
    expect(element(by.id('minInstances')).isPresent()).toBe(false);
    expect(element(by.id('initialInstances')).isPresent()).toBe(false);

    //deploying and scaling
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
    topologyEditorCommon.addScalingPolicy('rect_Ubuntu', 1, 3, 3);

    // applications.deployExistingApplication('Alien');
    applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);
    navigation.go('applications', 'runtime');

    // Wait for mock deployment to finish
    browser.sleep(10000);

    checkAndScale('rect_Compute',2 , 1);
    browser.sleep(10000);
    checkAndScale('rect_Compute',1);
    checkAndScale('rect_Compute',1, 2, true);
    browser.sleep(10000);
    checkAndScale('rect_Compute',1);

    checkAndScale('rect_Ubuntu', 3, 1);
    browser.sleep(10000);
    checkAndScale('rect_Ubuntu', 1);
    checkAndScale('rect_Ubuntu', 1, 2, true);
    browser.sleep(10000);
    checkAndScale('rect_Ubuntu', 1);
  });

});
