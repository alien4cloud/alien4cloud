/* global by, element */
'use strict';

var applications = require('../applications/applications');
var components = require('../components/components');
var authentication = require('../authentication/authentication');
var common = require('../common/common');
var cloudsCommon = require('../admin/clouds_common');
var navigation = require('../common/navigation');
var cloudImageCommon = require('../admin/cloud_image');

var relationshipTypesByIds = {
  'hostedOn10': 'tosca.relationships.HostedOn:1.0',
  'dependsOn10': 'tosca.relationships.DependsOn:1.0'
};
module.exports.relationshipTypesByIds = relationshipTypesByIds;

var topologyTemplates = {
  'template1': {
    'tName': 'WarTemplate',
    'tDescription': 'Simple architecture with one JAVA, one COMPUTE and a WAR'
  },
  'template2': {
    'tName': 'SoftwareFactory',
    'tDescription': 'Continous integration platform'
  },
  'template3': {
    'tName': 'SoftwareFactory-Release',
    'tDescription': 'Continous integration platform with Jenkins, Sonar, git en version RELEASE..'
  },
  'template4': {
    'tName': 'JavaTomcatWar',
    'tDescription': 'Simple server with one compoue, java installed and tomcat running'
  }
};
module.exports.topologyTemplates = topologyTemplates;


// Action to be executed before each topology test
var beforeTopologyTest = function() {
  common.before();
  authentication.login('admin');
  cloudImageCommon.addNewCloudImage('Windows', 'windows', 'x86_64', 'Windows', '14.04', '1', '1', '1');
  cloudImageCommon.addNewCloudImage('Ubuntu', 'linux', 'x86_64', 'Ubuntu', '14.04', '1', '1', '1');
  cloudsCommon.goToCloudList();
  cloudsCommon.createNewCloud('testcloud');
  cloudsCommon.goToCloudDetail('testcloud');
  cloudsCommon.enableCloud();
  cloudsCommon.addNewFlavor('medium', '12', '480', '4096');
//  cloudsCommon.selectFirstImageOfCloud();
//  cloudsCommon.selectFirstImageOfCloud();
  cloudsCommon.selectAllImageOfCloud();
  cloudsCommon.assignPaaSResourceToTemplate('Windows', 'medium', 'MEDIUM_WINDOWS');
  cloudsCommon.assignPaaSResourceToTemplate('Ubuntu', 'medium', 'MEDIUM_UBUNTU');
  authentication.logout();

  authentication.login('applicationManager');
  applications.createApplication('Alien', 'Great Application');
  // Go to the app details page
  browser.element(by.binding('application.name')).click();
  navigation.go('applications', 'topology');
};
module.exports.beforeTopologyTest = beforeTopologyTest;

// Show "Add node template" tab
function showComponentsTab() {
  var componentsSearchTab = element(by.id('components-search'));
  expect(componentsSearchTab.isPresent()).toBe(true);
  componentsSearchTab.click();
  browser.waitForAngular();
}

// Add a node template
var addNodeTemplate = function(ntype, expectedId, archiveVersion, selectedVersion) {
  showComponentsTab();

  // search element before selection
  var searchImput = element(by.model('searchedKeyword'));
  // TODO : should search by any word in the nodetype BUT the type is not
  // analyzed
  // var nodeNameToSearch = expectedId.split('_');
  // searchImput.sendKeys(nodeNameToSearch[1]); // e.g. Java or JavaRPM...
  searchImput.sendKeys(ntype); // e.g. tosca.nodes.Network
  var btnSearch = element(by.id('btn-search-component'));
  btnSearch.click();
  common.removeAllFacetFilters();

  // select and dnd the element
  var version = archiveVersion ? archiveVersion : '1.0';
  var nodeTypeElement = element(by.id('li_' + ntype + ':' + version));
  if (selectedVersion) {
    components.selectComponentVersion(ntype + ':' + version, selectedVersion);
    nodeTypeElement = element(by.id('li_' + ntype + ':' + selectedVersion));
  }
  var topologyVisuElement = element(by.id('topologySvgContainer'));
  // drag and drop is not supported by selenium so we hack a bit there...
  browser.driver
    .executeScript(
    '\
var typeScope = angular.element(arguments[0]).scope();\
var mainScope = angular.element(arguments[1]).scope();\
mainScope.nodeTypeSelected(typeScope.component);',
    nodeTypeElement.getWebElement(), topologyVisuElement.getWebElement()).then(function() {
      browser.waitForAngular();
    });
  browser.waitForAngular();

  var createdNode = element(by.id(expectedId));
  expect(createdNode.isDisplayed()).toBe(true);

  // clean search before next addNodeTemplate
  searchImput.clear(); // e.g. Java or JavaRPM...
  btnSearch.click();

  return createdNode;

};
module.exports.addNodeTemplate = addNodeTemplate;

var centerAndZoomOut = function() {
  var centerButton = browser.element(by.id('btn-topology-reset'));
  centerButton.click();
};
module.exports.centerAndZoomOut = centerAndZoomOut;

var checkTodoList = function(enabled) {
  navigation.go('applications', 'deployment');
  expect(element(by.binding('APPLICATIONS.TOPOLOGY.TASK.LABEL')).isPresent()).toBe(enabled);
  navigation.go('applications', 'topology');
  browser.waitForAngular();
};
module.exports.checkTodoList = checkTodoList;

var checkApplicationDeployable = function(applicationName, enabled) {
  common.goToApplicationSearchPage();
  common.goToApplicationDetailPage(applicationName);
  expect(element(by.css('button.application-deploy-button')).isEnabled()).toBe(enabled);
  expect(element(by.binding('APPLICATIONS.TOPOLOGY.TASK.LABEL')).isPresent()).toBe(!enabled);
  element(by.binding('MODIFY')).click();
};
module.exports.checkApplicationDeployable = checkApplicationDeployable;

var createTopologyWithNodesTemplates = function(nodeTemplateObj) {
  showComponentsTab();
  // Adding node to the topology specific topology
  Object.keys(nodeTemplateObj).forEach(function(nodeKey) {
    addNodeTemplate(nodeTemplateObj[nodeKey].type, nodeTemplateObj[nodeKey].id, nodeTemplateObj[nodeKey].archiveVersion, nodeTemplateObj[nodeKey].selectedVersion);
  });
  browser.waitForAngular();
};

var removeNodeTemplate = function(nodeName) {
  var node = element(by.id('rect_' + nodeName));
  node.click();
  common.deleteWithConfirm('btn-delete-node', true);
};

module.exports.removeNodeTemplate = removeNodeTemplate;

var btnRelationshipNameBaseId = 'btn-add-relationship-';
module.exports.btnRelationshipNameBaseId = btnRelationshipNameBaseId;


function addRelationshipSelectRelationship(relationshipTypeId, relationName, newVersion, newId) {
  // select a relationshipType
  var relationOnSelection = browser.element(by.id('li_' + relationshipTypeId));
  if (newVersion) {
    common.chooseSelectOption(relationOnSelection, newVersion);
    relationOnSelection = browser.element(by.id('li_' + newId));
  }
  relationOnSelection.click();

  if (relationName) {
    // change the name
    var relationshipNameInput = browser.element(by.model('relationshipModalData.name'));
    relationshipNameInput.clear();
    relationshipNameInput.sendKeys(relationName);
  }

  // If there is no "Finish" button click on cancel
  var btnFinish = browser.element(by.id('btn-modal-finish'));
  browser.actions().click(btnFinish).perform();
  browser.waitForAngular();
}

function addRelationshipCancel() {
  var btnCancel = browser.element(by.id('btn-modal-cancel'));
  browser.actions().click(btnCancel).perform();
  browser.waitForAngular();
}

function addRelationshipSelectCapability(targetNumber, targetNodeTemplateName, targetedCapabilityName, relationshipTypeId, relationName, newVersion, newId) {
  // no target => cancel
  if (targetNumber === 0) {
    addRelationshipCancel();
  } else {
    //if a capability is specified
    if (targetedCapabilityName && targetedCapabilityName.trim() !== '') {
      var capability = element(by.name(targetNodeTemplateName + '_' + targetedCapabilityName));
      // capability.click();
      browser.actions().click(capability).perform();
      addRelationshipSelectRelationship(relationshipTypeId, relationName, newVersion, newId);
    } else {
      //select the first capability
      var targetCapabilities = element(by.name(targetNodeTemplateName + '_capabilities'));
      targetCapabilities.isPresent().then(function(present) {
        if (present) {
          var capabilities = targetCapabilities.all(by.repeater('capability in  match.capabilities'));
          capabilities.first().click();
          addRelationshipSelectRelationship(relationshipTypeId, relationName, newVersion, newId);
        } else {
          addRelationshipCancel();
        }
      });
    }
  }
}

function addRelationshipToNode(sourceNodeTemplateName, targetNodeTemplateName, requirementName, relationshipTypeId, relationName, targetedCapabilityName, newVersion, newId) {
  // select the node template
  var sourceNode = element(by.id('rect_' + sourceNodeTemplateName));
  sourceNode.click();

  // select the requirement type
  var btnAddRelationship = browser.element(by.id(btnRelationshipNameBaseId + requirementName));
  browser.actions().click(btnAddRelationship).perform();
  browser.waitForAngular();

  var availableTargetsList = element.all(by.repeater('match in targets'));
  availableTargetsList.count().then(function(targetNumber) {
    addRelationshipSelectCapability(targetNumber, targetNodeTemplateName, targetedCapabilityName, relationshipTypeId, relationName, newVersion, newId);
  });
}
module.exports.addRelationshipToNode = addRelationshipToNode;

module.exports.addRelationship = function(relationshipDescription) {
  addRelationshipToNode(relationshipDescription.source, relationshipDescription.target, relationshipDescription.requirement,
    relationshipDescription.type, relationshipDescription.name, relationshipDescription.capability);
};

// check if a text is present in a repeater list
var checkCreatedRelationship = function(relationshipsNameStart, relationshipsCount) {
  var countRelationship = 0;
  var relationships = element.all(by.repeater('(relationshipName, relationshipDefinition) in selectedNodeTemplate.relationships'));
  browser.waitForAngular();

  // get a relationship array
  var relationshipList = relationships.map(function(elem, index) {
    return {
      relationshipIndex: index,
      relationshipName: elem.element(by.tagName('span')).getText()
    };
  });
  browser.waitForAngular();

  // when my relationship array is ready i do some test on it
  relationshipList.then(function(arrayRelationship) {

    // Testing relationshipsNameStart count
    arrayRelationship.forEach(function(relationship) {
      if (relationship.relationshipName.replace(/ /g, '').search(relationshipsNameStart) > -1) {
        countRelationship++;
      }
    });

    // test expected size
    expect(countRelationship).toBe(relationshipsCount);
  });

};
module.exports.checkCreatedRelationship = checkCreatedRelationship;

var addNodeTemplatesCenterAndZoom = function(nodesObject) {
  createTopologyWithNodesTemplates(nodesObject);
};
module.exports.addNodeTemplatesCenterAndZoom = addNodeTemplatesCenterAndZoom;

var removeRelationship = function(relationshipName) {
  common.deleteWithConfirm('btn-delete-rl-' + relationshipName, true);
};

module.exports.removeRelationship = removeRelationship;

var replaceNodeTemplates = function(nodeName, replacementElementId) {
  var node = element(by.id('rect_' + nodeName));
  node.click();
  common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
    element(by.css('.btn[ng-click^="getPossibleReplacements"]')).click();
    element(by.id('newnode_' + replacementElementId)).click();
  });
};
module.exports.replaceNodeTemplates = replaceNodeTemplates;

var checkNodeWasReplaced = function(nodeName, newNodeName) {
  // check the swap went well
  expect(element(by.id('rect_' + nodeName)).isPresent()).toBe(false);
  expect(element(by.id('rect_' + newNodeName)).isPresent()).toBe(true);
};
module.exports.checkNodeWasReplaced = checkNodeWasReplaced;

var addScalingPolicy = function(computeId, min, init, max) {

  var nodeToEdit = browser.element(by.id(computeId));
  browser.actions().click(nodeToEdit).perform();
  var scaleButton = browser.element(by.id('scaleButton'));
  browser.actions().click(scaleButton).perform();

  common.sendValueToXEditable('maxInstances', max, false);
  common.sendValueToXEditable('initialInstances', init, false);
  common.sendValueToXEditable('minInstances', min, false);
};
module.exports.addScalingPolicy = addScalingPolicy;

var removeScalingPolicy = function(computeId) {

  common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
    // Check properties edition on compute node template
    var nodeToEdit = browser.element(by.id(computeId));
    browser.actions().click(nodeToEdit).perform();
    var unscaleButton = browser.element(by.id('unscaleButton'));
    browser.actions().click(unscaleButton).perform();
  });
};

module.exports.removeScalingPolicy = removeScalingPolicy;

var selectNodeAndCheckProperty = function(nodeTemplateName, propertyName) {
  var nodeToEdit = browser.element(by.id('rect_' + nodeTemplateName));
  browser.actions().click(nodeToEdit).perform();

  var propertyElement = element(by.id('p_' + propertyName));
  expect(propertyElement.isPresent()).toBe(true);
};

var editNodeProperty = function(nodeTemplateName, propertyName, propertyValue) {

  selectNodeAndCheckProperty(nodeTemplateName, propertyName);

  var propertyElement = element(by.id('p_' + propertyName));
  var spanPropertyValue = propertyElement.element(by.tagName('span'));
  spanPropertyValue.click();

  var editForm = propertyElement.element(by.tagName('form'));
  var inputValue = editForm.element(by.tagName('input'));

  inputValue.clear();
  inputValue.sendKeys(propertyValue);
  editForm.submit();
  browser.waitForAngular();

};
module.exports.editNodeProperty = editNodeProperty;

// check if a text is present in the error message while editing a property
var checkPropertyEditionError = function(nodeTemplateName, propertyName, containedInErrorText) {
  browser.waitForAngular();
  var propertyElement = element(by.id('p_' + propertyName));
  var formElement = propertyElement.element(by.tagName('form'));

  // getting error div under the input
  var divError = formElement.element(by.tagName('div'));
  expect(divError.isDisplayed()).toBe(true);
  expect(divError.getText()).not.toEqual('');
  expect(divError.getText()).toContain(containedInErrorText);
  common.dismissAlertIfPresent();
};
module.exports.checkPropertyEditionError = checkPropertyEditionError;

var beforeToggle = function(nodeTemplateName) {
  var nodeToEdit = browser.element(by.id('rect_' + nodeTemplateName));
  browser.actions().click(nodeToEdit).perform();
  browser.waitForAngular();
};
var toggleIOProperty = function(nodeTemplateName, propertyName, ioType) {
  common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
    beforeToggle(nodeTemplateName);
    var ioButton = browser.element(by.id('p_' + ioType + '_' + propertyName));
    browser.actions().click(ioButton).perform();
  });
};

var expectIOPropertyState = function(nodeTemplateName, propertyName, ioType, checked) {
  var nodeToEdit = browser.element(by.id('rect_' + nodeTemplateName));
  browser.actions().click(nodeToEdit).perform();
  var ioButton = browser.element(by.id('p_' + ioType + '_' + propertyName));
  if (checked) {
    expect(ioButton.getAttribute('class')).toContain('active');
  } else {
    expect(ioButton.getAttribute('class')).not.toContain('active');
  }
};

var togglePropertyInput = function(nodeTemplateName, propertyName) {
  toggleIOProperty(nodeTemplateName, propertyName, 'input');
};

module.exports.togglePropertyInput = togglePropertyInput;

var togglePropertyOutput = function(nodeTemplateName, propertyName) {
  toggleIOProperty(nodeTemplateName, propertyName, 'output');
};

module.exports.togglePropertyOutput = togglePropertyOutput;

var toggleAttributeOutput = function(nodeTemplateName, attributeName) {
  beforeToggle(nodeTemplateName);
  common.ptor.executeScript('window.scrollTo(0,0);').then(function() {
    var ioButton = browser.element(by.id('a_output_' + attributeName));
    browser.actions().click(ioButton).perform();
  });
};

module.exports.toggleAttributeOutput = toggleAttributeOutput;

var expectPropertyInputState = function(nodeTemplateName, propertyName, checked) {
  expectIOPropertyState(nodeTemplateName, propertyName, 'input', checked);
};

module.exports.expectPropertyInputState = expectPropertyInputState;

var expectPropertyOutputState = function(nodeTemplateName, propertyName, checked) {
  expectIOPropertyState(nodeTemplateName, propertyName, 'output', checked);
};

module.exports.expectPropertyOutputState = expectPropertyOutputState;

var expectAttributeOutputState = function(nodeTemplateName, propertyName, checked) {
  var nodeToEdit = browser.element(by.id('rect_' + nodeTemplateName));
  browser.actions().click(nodeToEdit).perform();
  var ioButton = browser.element(by.id('a_output_' + propertyName));
  if (checked) {
    expect(ioButton.getAttribute('class')).toContain('active');
  } else {
    expect(ioButton.getAttribute('class')).not.toContain('active');
  }
};
module.exports.expectAttributeOutputState = expectAttributeOutputState;

var checkNumberOfRelationship = function(expectedCount) {
  var relationships = element.all(by.repeater('(relationshipName, relationshipDefinition) in selectedNodeTemplate.relationships'));
  browser.waitForAngular();
  expect(relationships.count()).toBe(expectedCount);
};
module.exports.checkNumberOfRelationship = checkNumberOfRelationship;

var checkNumberOfRelationshipForANode = function(nodeName, expectedCount) {
  element(by.id(nodeName)).click();
  browser.waitForAngular();
  checkNumberOfRelationship(expectedCount);
};
module.exports.checkNumberOfRelationshipForANode = checkNumberOfRelationshipForANode;
