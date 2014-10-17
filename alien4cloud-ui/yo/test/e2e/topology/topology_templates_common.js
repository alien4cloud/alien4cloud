/* global by, element */

'use strict';

var navigation = require('../common/navigation');
var topologyEditorCommon = require('./topology_editor_common');
var componentData = require('./component_data');

module.exports.goToTemplateDetailPage = function(templateName) {
  navigation.go('main', 'topologyTemplates');
  var templateElement = element(by.id('template_' + templateName));
  templateElement.click();
  browser.waitForAngular();
};

// Expose create topology template functions
function createTopologyTemplate(newTemplateName, newTemplateDescription) {
  navigation.go('main', 'topologyTemplates');

  // Can add a new topology template
  var btnNewTemplate = browser.element(by.binding('TEMPLATE.NEW'));
  browser.actions().click(btnNewTemplate).perform();

  element(by.model('topologytemplate.name')).sendKeys(newTemplateName);
  element(by.model('topologytemplate.description')).sendKeys(newTemplateDescription);

  // Create a topology and find it in the list
  var btnCreate = browser.element(by.binding('CREATE'));
  browser.actions().click(btnCreate).perform();
  browser.waitForAngular();
}
module.exports.createTopologyTemplate = createTopologyTemplate;

function createTopologyTemplateWithNodes(nodeTemplateObj) {
  // Adding node to the topology
  Object.keys(nodeTemplateObj).forEach(function(nodeKey) {
    topologyEditorCommon.addNodeTemplate(nodeTemplateObj[nodeKey].type, nodeTemplateObj[nodeKey].id, nodeTemplateObj[nodeKey].archiveVersion, nodeTemplateObj[nodeKey].selectedVersion);
  });
  browser.waitForAngular();
}

module.exports.createTopologyTemplateWithNodesAndRelationships = function(topologyTemplateObj) {
  // create the topology template
  createTopologyTemplate(topologyTemplateObj.tName, topologyTemplateObj.tDescription);

  createTopologyTemplateWithNodes(componentData.simpleTopology.nodes);

  topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
  topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.dependsOnCompute2);

  // check relationship count
  element(by.id('rect_JavaRPM')).click();
  browser.waitForAngular();
  var relationships = element.all(by.repeater('(relationshipName,relationshipDefinition) in selectedNodeTemplate.relationships'));
  expect(relationships.count()).toBe(2);
  element(by.id('rect_Compute_2')).click();
  browser.waitForAngular();
  relationships = element.all(by.repeater('(relationshipName,relationshipDefinition) in selectedNodeTemplate.relationships'));
  expect(relationships.count()).toBe(0);
  element(by.id('rect_Compute')).click();
  browser.waitForAngular();
  relationships = element.all(by.repeater('(relationshipName,relationshipDefinition) in selectedNodeTemplate.relationships'));
  expect(relationships.count()).toBe(0);
};
