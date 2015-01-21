/* global element, by */

'use strict';
var common = require('../common/common');
var navigation = require('../common/navigation');
var applications = require('../applications/applications');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');

var computesNodeTemplates = {
    compute: componentData.toscaBaseTypes.compute(),
    ubuntu: componentData.ubuntuTypes.ubuntu()
};

var scale = function(newValue) {
  var scaleEditableInput = element(by.id('scaleEditableInput'));
  var scaleEditableButton = element(by.id('scaleEditableButton'));
  scaleEditableButton.click();
  var editForm = scaleEditableInput.element(by.tagName('form'));
  var editInput = editForm.element(by.tagName('input'));
  editInput.clear();
  editInput.sendKeys(newValue);
  browser.waitForAngular();
  editForm.submit();
  browser.waitForAngular();
  expect(scaleEditableInput.getText()).toContain(newValue);
};

var checkAndScale = function(nodeId, valueToCheck, newValue){
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
        scale(newValue);
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

  it('should be able to add scaling policy, deploy and scale a compute, and every node derived from it', function() {
    console.log('################# should be able to add scaling policy, deploy and scale a compute, and every node derived from it');
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
    
    applications.deployExistingApplication('Alien');

    // Wait for mock deployment to finish
    browser.sleep(10000);

    checkAndScale('rect_Compute',2 , 1);
    browser.sleep(10000);
    checkAndScale('rect_Compute',1);
    
    checkAndScale('rect_Ubuntu', 3, 1);
    browser.sleep(10000);
    checkAndScale('rect_Ubuntu', 1);
  });
  
});
