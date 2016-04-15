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
  // show components tab
  topologyEditorCommon.showComponentsTab();
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
  topologyEditorCommon.checkNumberOfRelationshipForANode('JavaRPM', 2);
  topologyEditorCommon.checkNumberOfRelationshipForANode('Compute_2', 0);
  topologyEditorCommon.checkNumberOfRelationshipForANode('Compute', 0);
};
