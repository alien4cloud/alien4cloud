/* global by, element, browser, expect */
'use strict';

var applications = require('../applications/applications');
var components = require('../components/components');
var authentication = require('../authentication/authentication');
var common = require('../common/common');
var toaster = require('../common/toaster');

var nodeDetailsBlockList = [
  'node-details-properties',
  'node-details-attributes',
  'node-details-requirements',
  'node-details-capabilities',
  'node-details-relationships',
  'node-details-artifacts',
  'node-details-scaling'
];

var nodeDetailsBlocsIds = {
  pro: 'node-details-properties',
  att: 'node-details-attributes',
  req: 'node-details-requirements',
  cap: 'node-details-capabilities',
  rel: 'node-details-relationships',
  art: 'node-details-artifacts',
  sca: 'node-details-scaling'
};
module.exports.nodeDetailsBlocsIds = nodeDetailsBlocsIds;


var go = function() {
  common.go('applications', 'topology');
};
module.exports.go = go;


// Show tabs in the topology page
function showTopologyTab(panel, btn) {
  element(by.id(panel)).isDisplayed().then(function(isDisplay) {
    if (!isDisplay) {
      browser.actions().click(element(by.id(btn)).element(by.tagName('i'))).perform();
    }
  });
  browser.waitForAngular();
}

function showComponentsTab() {
  showTopologyTab('closeComponentsSearch', 'topology-components-search');
}
module.exports.showComponentsTab = showComponentsTab;

function showDependenciesTab() {
  showTopologyTab('closeDependencies', 'topology-dependencies');
}
module.exports.showDependenciesTab = showDependenciesTab;

function showInputsTab() {
  showTopologyTab('closeInputs', 'topology-inputs');
}
module.exports.showInputsTab = showInputsTab;

function closeInputsTab() {
  element(by.id('closeInputs')).isDisplayed().then(function(isDisplay) {
    if (isDisplay) {
      element(by.id('closeInputs')).click();

    }
  });
  browser.waitForAngular();
}
module.exports.closeInputsTab = closeInputsTab;

// Add a node template
var addNodeTemplate = function(nodeType, expectedId, archiveVersion, selectedVersion) {
  searchComponents(nodeType);
  // select and dnd the element
  var version = archiveVersion ? archiveVersion : '1.0';
  var nodeTypeElement = element(by.id('li_' + nodeType + ':' + version));
  if (selectedVersion) {
    components.selectComponentVersion(nodeType + ':' + version, selectedVersion);
    nodeTypeElement = element(by.id('li_' + nodeType + ':' + selectedVersion));
  }
  var topologyVisualElement = element(by.id('topologySvgContainer'));
  // drag and drop is not supported by selenium so we hack a bit there...
  browser.driver
    .executeScript(
      '\
  var typeScope = angular.element(arguments[0]).scope();\
  var mainScope = angular.element(arguments[1]).scope();\
  mainScope.nodes.add(typeScope.component);',
      nodeTypeElement.getWebElement(), topologyVisualElement.getWebElement()).then(function() {
    browser.waitForAngular();
  });
  browser.waitForAngular();

  var createdNode = element(by.id(expectedId));
  expect(createdNode.isDisplayed()).toBe(true);
  return createdNode;
};

module.exports.addNodeTemplate = addNodeTemplate;

var searchComponents = function(nodeType) {
  showComponentsTab();
  // search element before selection
  common.sendKeys(by.css('#comp-search-side-panel input'), nodeType);
  var btnSearch = element(by.id('btn-search-component'));
  browser.actions().click(btnSearch).perform();
};

module.exports.searchComponents = searchComponents;

var centerAndZoomOut = function() {
  var centerButton = element(by.id('btn-topology-reset'));
  browser.actions().click(centerButton).perform();
};
module.exports.centerAndZoomOut = centerAndZoomOut;

var checkTodoList = function(enabled) {
  common.go('applications', 'deployment');
  expect(element(by.binding('APPLICATIONS.TOPOLOGY.TASK.LABEL')).isPresent()).toBe(enabled);
  common.go('applications', 'topology');
  browser.waitForAngular();
};
module.exports.checkTodoList = checkTodoList;

var checkWarningList = function(enabled) {
  common.go('applications', 'deployment');
  expect(element(by.binding('APPLICATIONS.TOPOLOGY.WARNING.LABEL')).isPresent()).toBe(enabled);
};
module.exports.checkWarningList = checkWarningList;

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
  browser.actions().click(node).perform();
  common.deleteWithConfirm('btn-delete-node', true);
};

module.exports.removeNodeTemplate = removeNodeTemplate;

var btnRelationshipNameBaseId = 'btn-add-relationship-';
module.exports.btnRelationshipNameBaseId = btnRelationshipNameBaseId;


function addRelationshipSelectRelationship(relationshipTypeId, relationName, newVersion, newId) {
  // select a relationshipType
  var relationOnSelection = browser.element(by.id('li_' + relationshipTypeId));
  relationOnSelection.isPresent().then(function(isPresent) {
    if (isPresent) {
      if (newVersion) {
        common.chooseSelectOption(relationOnSelection, newVersion);
        relationOnSelection = browser.element(by.id('li_' + newId));
      }
      relationOnSelection.click();
    }
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
  });
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
  selectNodeAndGoToDetailBloc(sourceNodeTemplateName, nodeDetailsBlocsIds.req);

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

  // display only one bloc in node details : relationship
  collapseNodeDetailsBloc(nodeDetailsBlocsIds.rel);

  var countRelationship = 0;
  var relationships = element.all(by.repeater('relationshipEntry in selectedNodeTemplate.relationships'));
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
  browser.actions().click(node).perform();
  browser.executeScript('window.scrollTo(0,0);').then(function() {
    browser.actions().click(element(by.css('.btn[ng-click^="nodesswap.getPossibleReplacements"]'))).perform();
    browser.actions().click(element(by.id('newnode_' + replacementElementId))).perform();
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
  editNodeProperty(computeId, 'min_instances', min, 'cap');
  editNodeProperty(computeId, 'max_instances', max, 'cap');
  editNodeProperty(computeId, 'default_instances', init, 'cap');
};
module.exports.addScalingPolicy = addScalingPolicy;

var removeScalingPolicy = function(computeId) {
  addScalingPolicy(computeId, 1, 1, 1);
};

module.exports.removeScalingPolicy = removeScalingPolicy;

var selectNodeAndGoToDetailBloc = function(nodeTemplateName, blocId) {
  common.click(by.id('rect_' + nodeTemplateName));
  if (blocId) {
    return collapseNodeDetailsBloc(blocId);
  }
};
module.exports.selectNodeAndGoToDetailBloc = selectNodeAndGoToDetailBloc;

var editNodeProperty = function(nodeTemplateName, propertyName, propertyValue, componentType, unit) {
  componentType = (componentType === undefined || componentType === null) ? 'pro' : componentType;
  showComponentsTab();
  selectNodeAndGoToDetailBloc(nodeTemplateName, nodeDetailsBlocsIds[componentType]);
  var propertyElement = common.element(by.id(nodeDetailsBlocsIds[componentType] + '-panel'));
  propertyElement = common.element(by.id('p_' + propertyName), propertyElement);
  var editForm;
  if (unit) {
    common.click(by.css('div button'), propertyElement);
    common.click(by.id('p_' + propertyName + '_unit_' + unit), propertyElement);
  }
  common.click(by.css('span[editable-text]'), propertyElement);
  editForm = common.element(by.tagName('form'), propertyElement);
  var inputValue = common.element(by.tagName('input'), editForm);

  inputValue.clear();
  inputValue.sendKeys(propertyValue);
  editForm.submit();
};
module.exports.editNodeProperty = editNodeProperty;

// check if a text is present in the error message while editing a property
var checkPropertyEditionError = function(nodeTemplateName, propertyName, containedInErrorText) {
  var propertyElement = element(by.id('p_' + propertyName));
  var formElement = propertyElement.element(by.tagName('form'));

  // getting error div under the input
  var divError = formElement.element(by.css('div.editable-error'));
  expect(divError.isDisplayed()).toBe(true);
  expect(divError.getText()).not.toEqual('');
  expect(divError.getText()).toContain(containedInErrorText);
  var input = propertyElement.element(by.tagName('input'));
  input.sendKeys(protractor.Key.ESCAPE);
  toaster.dismissIfPresent();
};
module.exports.checkPropertyEditionError = checkPropertyEditionError;

var toggleIOProperty = function(nodeTemplateName, propertyName, ioType, componentType) {
  selectNodeAndGoToDetailBloc(nodeTemplateName, nodeDetailsBlocsIds[componentType]);
  var ioButton = common.element(by.id('p_' + ioType + '_' + componentType + '_' + propertyName));
  browser.actions().click(ioButton).perform();
};

var expectIOPropertyState = function(nodeTemplateName, propertyName, ioType, checked, componentType) {
  componentType = (componentType === undefined || componentType === null) ? 'pro' : componentType;
  selectNodeAndGoToDetailBloc(nodeTemplateName, nodeDetailsBlocsIds.pro);
  var ioButton = browser.element(by.id('p_' + ioType + '_' + componentType + '_' + propertyName));
  if (checked) {
    expect(ioButton.getAttribute('class')).toContain('active');
  } else {
    expect(ioButton.getAttribute('class')).not.toContain('active');
  }
};

var removeInput = function(inputName) {
  showInputsTab();
  common.deleteWithConfirm(inputName + '_delete', true);
  closeInputsTab();
};
module.exports.removeInput = removeInput;

var renameInput = function(oldInputName, newInputName) {
  showInputsTab();
  common.sendValueToXEditable('td_' + oldInputName, newInputName, false);
  closeInputsTab();
};
module.exports.renameInput = renameInput;

var togglePropertyInput = function(nodeTemplateName, propertyName, componentType) {
  componentType = (componentType === undefined || componentType === null) ? 'pro' : componentType;
  toggleIOProperty(nodeTemplateName, propertyName, 'input', componentType);
  common.click(by.id('addToInputBtn_' + componentType + '_' + propertyName));
};
module.exports.togglePropertyInput = togglePropertyInput;

var associatePropertyToInput = function(nodeTemplateName, propertyName, inputId, componentType) {
  componentType = (componentType === undefined || componentType === null) ? 'pro' : componentType;
  toggleIOProperty(nodeTemplateName, propertyName, 'input', componentType);
  common.click(by.id(nodeTemplateName + '_' + propertyName + '_toAssociate_' + inputId));
};
module.exports.associatePropertyToInput = associatePropertyToInput;

var togglePropertyOutput = function(nodeTemplateName, propertyName, componentType) {
  componentType = (componentType === undefined || componentType === null) ? 'pro' : componentType;
  toggleIOProperty(nodeTemplateName, propertyName, 'output', componentType);
};

module.exports.togglePropertyOutput = togglePropertyOutput;

var toggleAttributeOutput = function(nodeTemplateName, attributeName) {
  selectNodeAndGoToDetailBloc(nodeTemplateName, nodeDetailsBlocsIds.att);
  browser.executeScript('window.scrollTo(0,0);').then(function() {
    var ioButton = browser.element(by.id('a_output_' + attributeName));
    browser.actions().click(ioButton).perform();
  });
};

module.exports.toggleAttributeOutput = toggleAttributeOutput;

var expectPropertyInputState = function(nodeTemplateName, propertyName, checked, componentType) {
  expectIOPropertyState(nodeTemplateName, propertyName, 'input', checked, componentType);
};

module.exports.expectPropertyInputState = expectPropertyInputState;

var expectPropertyOutputState = function(nodeTemplateName, propertyName, checked, componentType) {
  expectIOPropertyState(nodeTemplateName, propertyName, 'output', checked, componentType);
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
  var relationships = element.all(by.repeater('relationshipEntry in selectedNodeTemplate.relationships'));
  browser.waitForAngular();
  expect(relationships.count()).toBe(expectedCount);
};
module.exports.checkNumberOfRelationship = checkNumberOfRelationship;

var checkNumberOfRelationshipForANode = function(nodeName, expectedCount) {
  selectNodeAndGoToDetailBloc(nodeName, nodeDetailsBlocsIds.rel);
  checkNumberOfRelationship(expectedCount);
};
module.exports.checkNumberOfRelationshipForANode = checkNumberOfRelationshipForANode;

var expectDeploymentWork = function(goToAppDetail, work) {
  if (goToAppDetail) {
    authentication.reLogin('applicationManager');
    applications.goToApplicationDetailPage('Alien');
    common.go('applications', 'deployment');
  }
  var deployButton = browser.element(by.binding('APPLICATIONS.DEPLOY'));
  browser.waitForAngular();
  if (work) {
    expect(deployButton.getAttribute('disabled')).toBeNull();
    expect(element(by.id('div-deployment-matcher')).element(by.tagName('legend')).element(by.tagName('i')).getAttribute('class')).not.toContain('text-danger');
  } else {
    expect(deployButton.getAttribute('disabled')).toEqual('true');
    expect(element(by.id('div-deployment-matcher')).element(by.tagName('legend')).element(by.tagName('i')).getAttribute('class')).toContain('text-danger');
  }
};
module.exports.expectDeploymentWork = expectDeploymentWork;

var expectShowTodoList = function(goToAppDetail, isDisplay) {
  if (goToAppDetail) {
    authentication.reLogin('applicationManager');
    applications.goToApplicationDetailPage('Alien');
    common.go('applications', 'deployment');
  }
  if (isDisplay) {
    expect(element(by.id('deploymentTodoList')).isPresent()).toBe(true);
  } else {
    expect(element(by.id('deploymentTodoList')).isPresent()).toBe(false);
  }
};
module.exports.expectShowTodoList = expectShowTodoList;

/** Close or open a specific node template details bloc */
var nodeDetailsCollapse = function(blocId, opened) {
  var myBlock = element(by.id(blocId));
  return myBlock.isPresent().then(function(blockPresent) {
    if (blockPresent) {
      var myBlockIcon = myBlock.element(by.tagName('i'));
      return myBlockIcon.getAttribute('class');
    }
  }).then(function(classes) {
    // test if the bloc is opened and then close it
    if (classes && ((opened === true && classes.split(' ').indexOf('fa-chevron-right') !== -1) || (opened === false && classes.split(' ').indexOf('fa-chevron-down') !== -1))) {
      browser.actions().click(myBlock).perform();
      return true;
    } else {
      return false;
    }
  });
};

var doCollapseNodeDetailBlock = function(blocId, nextBlocIndex) {
  if (nextBlocIndex >= nodeDetailsBlockList.length) {
    // End of the node details block list
  } else if (blocId === nodeDetailsBlockList[nextBlocIndex]) {
    // Required bloc found do not loop anymore
    return nodeDetailsCollapse(blocId, true);
  } else {
    // Continue to collapse until we found the block
    return nodeDetailsCollapse(nodeDetailsBlockList[nextBlocIndex], false).then(function() {
      return doCollapseNodeDetailBlock(blocId, nextBlocIndex + 1);
    });
  }
};

/** Open only one bloc in the node template details */
var collapseNodeDetailsBloc = function(blocId) {
  // Close all details
  return doCollapseNodeDetailBlock(blocId, 0);
};
module.exports.collapseNodeDetailsBloc = collapseNodeDetailsBloc;

var checkCountInputs = function(valueExpected) {
  showInputsTab();
  element.all(by.repeater('(inputId, inputDefinition) in topology.topology.inputs')).then(function(inputs) {
    expect(inputs.length).toEqual(valueExpected);
  });
  closeInputsTab();
};
module.exports.checkCountInputs = checkCountInputs;

var renameApplicationInput = function(oldName, newName) {
  showInputsTab();
  common.sendValueToXEditable('td_' + oldName, newName, false);
  closeInputsTab();
};
module.exports.renameApplicationInput = renameApplicationInput;

var checkNumberOfPropertiesForACapability = function(capabilityName, expectedCount) {
  var capability = element(by.id('node-details-capabilities-' + capabilityName + '-block'));
  var capabilityProperties = capability.all(by.repeater('propertyEntry in capabilityEntry.value.properties'));
  browser.waitForAngular();
  expect(capabilityProperties.count()).toBe(expectedCount);
};
module.exports.checkNumberOfPropertiesForACapability = checkNumberOfPropertiesForACapability;

var addNodeTemplateToNodeGroup = function(nodeTemplateName, groupName) {
  // click on the node image
  browser.actions().click(element(by.id('rect_' + nodeTemplateName))).perform();

  // click on the group icon
  browser.actions().click(element(by.id('node_groups_' + nodeTemplateName))).perform();
  if (groupName) {
    // associate the node to the group
    browser.actions().click(element(by.id('Compute_memberOf_' + groupName))).perform();
  } else {
    // add a new group
    browser.actions().click(element(by.id('createGroupWithMember_' + nodeTemplateName))).perform();
  }
};
module.exports.addNodeTemplateToNodeGroup = addNodeTemplateToNodeGroup;
