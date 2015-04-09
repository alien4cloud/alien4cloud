/* global element, by */
'use strict';

var common = require('../../common/common');
var navigation = require('../../common/navigation');
var applications = require('../../applications/applications');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

var nodeTemplates = {
  java: componentData.fcTypes.javaRPM(),
  compute: componentData.toscaBaseTypes.compute()
};

var operationNodeTemplates = {
  apacheLBGroovy: componentData.apacheTypes.apacheLBGroovy(),
  compute: componentData.toscaBaseTypes.compute()
};

var goToAlienAppAndSelectApachelbOperations = function() {

  applications.goToApplicationDetailPage('Alien');
  navigation.go('applications', 'topology');
  topologyEditorCommon.addNodeTemplatesCenterAndZoom(operationNodeTemplates);
  topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
  topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
  topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
  topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
  browser.executeScript('window.scrollTo(0,0);').then(function() {
    topologyEditorCommon.addRelationshipToNode('apacheLBGroovy', 'Compute', 'host', 'tosca.relationships.HostedOn:2.0', 'hostedOnComputeHost');
  });

  // applications.deployExistingApplication('Alien');
  applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);
  navigation.go('applications', 'runtime');

  // go to operations tab on apacheLBGroovy node
  var apacheNode = element(by.id('rect_apacheLBGroovy'));
  apacheNode.click();

};

describe('Topology runtime view', function() {

  beforeEach(function() {
    topologyEditorCommon.beforeTopologyTest();
  });

  afterEach(function() {
    common.after();
  });

  it('should be able to add scaling policy to a compute node', function() {
    console.log('################# should be able to add scaling policy to a compute node');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(nodeTemplates);
    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
    browser.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.removeScalingPolicy('rect_Compute');
    });
    browser.sleep(1000);
    expect(element(by.id('maxInstances')).isPresent()).toBe(false);
    expect(element(by.id('minInstances')).isPresent()).toBe(false);
    expect(element(by.id('initialInstances')).isPresent()).toBe(false);
  });

  it('should be able to view topology runtime view', function() {
    console.log('################# should be able to view topology runtime view');
    topologyEditorCommon.addNodeTemplatesCenterAndZoom(nodeTemplates);
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.editNodeProperty('Compute', 'containee_types', 'test', 'cap');
    topologyEditorCommon.addScalingPolicy('rect_Compute', 1, 2, 3);
    browser.executeScript('window.scrollTo(0,0);').then(function() {
      topologyEditorCommon.addRelationshipToNode('JavaRPM', 'Compute', 'host', 'tosca.relationships.HostedOn:2.0', 'hostedOnComputeHost');
    });

    applications.deploy('Alien', null, null, null, applications.mockPaaSDeploymentProperties);
    navigation.go('applications', 'runtime');
    element(by.id('events-tab')).click();
    // Wait for mock deployment to finish
    browser.sleep(10000);
    element.all(by.repeater('event in events.data | orderBy:\'date\':true')).then(function(allEvents) {
      expect(allEvents.length).toBeGreaterThan(0);
      element(by.id('eventTypeSelection')).click();
      element(by.id('paasdeploymentstatusmonitoreventSelect')).click();
      browser.waitForAngular();
      element.all(by.repeater('event in events.data | orderBy:\'date\':true')).then(function(statusEvents) {
        element(by.id('eventTypeSelection')).click();
        element(by.id('paasinstancestatemonitoreventSelect')).click();
        browser.waitForAngular();
        element.all(by.repeater('event in events.data | orderBy:\'date\':true')).then(function(instanceStateEvents) {
          element(by.id('eventTypeSelection')).click();
          element(by.id('paasmessagemonitoreventSelect')).click();
          browser.waitForAngular();
          element.all(by.repeater('event in events.data | orderBy:\'date\':true')).then(function(messageEvents) {
            expect(messageEvents.length + instanceStateEvents.length + statusEvents.length).toEqual(allEvents.length);
          });
        });
      });
    });

    var nodeToView = element(by.id('rect_Compute'));
    nodeToView.click();
    element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).then(function(states) {
      expect(states.length).toEqual(2);
      states[0].element(by.css('.btn-default')).click(); // the view instance details button
      expect(element.all(by.repeater('(propKey, propVal) in selectedInstance.runtimeProperties')).count()).toEqual(1);
      var backButton = browser.element(by.id('backToInstanceListButton'));
      browser.actions().click(backButton).perform();
      element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).then(function(states) {
        expect(states.length).toEqual(2);
      });
    });
  });


  it('should be able to trigger the execution of an operation (custom command) on an instance of a node', function() {
    console.log('################# should be able to trigger the execution of an operation (custom command) on an instance of a node');
    // jump to Apache LB operations tab
    goToAlienAppAndSelectApachelbOperations();

    // trigger operation without params: removeNode
    var operationDiv = element(by.id('operation_removeNode'));
    var submitOperationBtn = operationDiv.element(by.id('btn-submit-operation-removeNode'));
    submitOperationBtn.click();
    browser.sleep(1000);
    common.dismissAlert();

    // trigger operation with params: addNode
    operationDiv = element(by.id('operation_addNode'));
    operationDiv.element(by.css('div.clickable')).click();
    browser.sleep(1000);
    // enter 2 values befor execute
    common.sendValueToXEditable('p_instanceId', '1', false);
    common.sendValueToXEditable('p_node', 'MyNodeName', false);

    // exdecute the addNode operation
    submitOperationBtn = operationDiv.element(by.id('btn-submit-operation-addNode'));
    submitOperationBtn.click();
    browser.sleep(1000);

    // test success result messages
    common.expectNoErrors();
    common.expectSuccess();
    common.expectTitleMessage('addNode');
    common.expectMessageContent('Instance 1 : OK');
    common.dismissAlert();

    // check operation result in <Details> tab
    element(by.id('details-tab')).click();
    element.all(by.repeater('(id, info) in topology.instances[selectedNodeTemplate.name]')).first();

  });

  it('should have and error message when excuting addNode operation without required parameters', function() {
    console.log('################# should have and error message when excuting addNode operation without required parameters');

    // jump to Apache LB operations tab
    goToAlienAppAndSelectApachelbOperations();

    // go on add operation details
    var operationDiv = element(by.id('operation_addNode'));
    operationDiv.element(by.css('div.clickable')).click();

    // trigger operation addNode : 2 params required
    operationDiv = element(by.id('operation_addNode'));
    var submitOperationBtn = operationDiv.element(by.id('btn-submit-operation-addNode'));
    submitOperationBtn.click();

    // should have error toaster
    common.expectErrors();
    common.expectMessageContent('node');
    common.expectMessageContent('instanceId');
    common.dismissAlert();

  });

  it('should have and error message when excuting removeNode with <operation failed> message', function() {
    console.log('################# should have and error message when excuting removeNode with <operation failed> message');

    // jump to Apache LB operations tab
    goToAlienAppAndSelectApachelbOperations();

    // trigger operation without params: removeNode
    var operationDiv = element(by.id('operation_removeNode'));
    var submitOperationBtn = operationDiv.element(by.id('btn-submit-operation-removeNode'));
    submitOperationBtn.click();

    // should have error toaster
    common.expectErrors();
    common.expectMessageContent('Operation execution message when failing');
    common.dismissAlert();

  });

});
