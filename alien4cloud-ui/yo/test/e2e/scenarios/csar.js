/**
 * CSAR functionality is not enabled for now...
 */

'use strict';

var common = require('../common/common');
var authentication = require('../authentication/authentication');
var genericForm = require('../generic_form/generic_form');
var csarCommon = require('../csars/csars_commons');

var componentManager = {
  'username': 'componentManager',
  'password': 'componentManager'
};

// create a first CSAR
var csar0 = {};
csar0.name = 'MyNewCSARName0';
csar0.version = '2.0';
csar0.description = 'First version for my CSAR....';

// create a second CSAR
var csar1 = {};
csar1.name = 'MyNewCSARName-3';
csar1.version = 'v4-3';
csar1.description = 'Brand new CSAR ....';

describe('Csar creation : ' + common.csarSearchUrl, function() {

  beforeEach(function() {
    common.before();
    authentication.login(componentManager.username, componentManager.password);
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to go to csar search page at ' + common.csarSearchUrl + ' and create new csar', function() {
    console.log('################# should be able to go to csar search page at ' + common.csarSearchUrl + ' and create new csar');
    // create a first CSAR
    csarCommon.createCsar(csar0.name, csar0.version, csar0.description);

    // create a second CSAR
    csarCommon.createCsar(csar1.name, csar1.version, csar1.description);

    var csars = element.all(by.repeater('csar in csarSearchResult.data.data'));
    expect(csars.count()).toEqual(2);

  });

  it('should add 2 csars and go on second csar details', function() {
    console.log('################# should add 2 csars and go on second csar details');
    csarCommon.goToCsarSearchPage();
    csarCommon.goToCsarDetails(1);
    expect(element(by.binding('csar.name')).getText()).toContain(csar1.name);
    expect(element(by.binding('csar.version')).getText()).toContain(csar1.version);
    expect(element(by.binding('csar.description')).getText()).toContain(csar1.description);
  });

  it('should add 2 csars and delete the second one', function() {
    console.log('################# should add 2 csars and delete the second one');
    csarCommon.goToCsarSearchPage();
    csarCommon.goToCsarDetails(1);
    // Delete second csar
    var csarId = 'csar_' + csar1.name + ':' + csar1.version + '-SNAPSHOT';
    common.deleteWithConfirm('delete-' + csarId, true);
    // after deletion -> redirect to #/csars
    browser.getCurrentUrl().then(function(url) {
      var firstElement = common.getUrlElement(url, 1);
      expect(firstElement).toEqual('csars');
    });

    var csars = element.all(by.repeater('csar in csarSearchResult.data.data'));
    expect(csars.count()).toEqual(1);

    csarCommon.goToCsarDetails(0);

    expect(element(by.binding('csar.name')).getText()).toContain(csar0.name);
    expect(element(by.binding('csar.version')).getText()).toContain('-SNAPSHOT');
    expect(element(by.binding('csar.version')).getText()).toContain(csar0.version);
    expect(element(by.binding('csar.description')).getText()).toContain(csar0.description);
  });

  it('should be able to add/edit/suppress a node type', function() {
    console.log('################# should be able to add/edit/suppress a node type');
    csarCommon.goToCsarSearchPage();
    csarCommon.goToCsarDetails(0);

    // Open add node type form button
    var btnAddNodeType = browser.element(by.binding('CSAR.DETAILS.BUTTON_ADD_NODE_TYPE'));

    var createNodeTypeForm = element(by.id('createNodeTypeForm'));

    var nodeTypeList = element(by.id('nodeTypeList'));

    expect(createNodeTypeForm.isPresent()).toBe(false);
    expect(nodeTypeList.isDisplayed()).toBe(false);

    // Open create node type form
    browser.actions().click(btnAddNodeType).perform();
    expect(createNodeTypeForm.isPresent()).toBe(true);
    expect(createNodeTypeForm.isDisplayed()).toBe(true);
    browser.waitForAngular();
    // Send value to a id field without auto-completion
    genericForm.sendValueToPrimitive('id', 'fr.fastconnect.myCompute', false, "xeditable");
    // Send value to derivedFrom field with auto-completion
    genericForm.sendValueToPrimitive('derivedFrom', 'tomcat', true, "xeditable");
    // Add a property with name cpu
    var cpuProperty = {
      'type': 'string',
      'required': true,
      // Problem with scope which became enumeration and not string anymore
      // Generic form do not manage enumeration for the moment
      // 'scope' : 'runtime',
      'default': 4,
      'description': 'Property which define the number of cpu core'
    };
    var cpuPropertyElementType = {
      'type': 'select',
      'required': 'radio',
      'default': 'xeditable',
      'description': 'xeditable'
    };

    genericForm.addKeyToMap('properties', 'cpu', cpuProperty, {}, cpuPropertyElementType);
    var computeRequirement = {
      'type': 'container',
      'lowerBound': 1,
      'upperBound': 1
    };
    var computeRequirementElementType = {
      'type': 'xeditable',
      'lowerBound': 'xeditable',
      'upperBound': 'xeditable'
    };
    genericForm.addKeyToMap('requirements', 'compute', computeRequirement, {
      'type': true
    }, computeRequirementElementType);
    var appServerCapability = {
      'type': 'container',
      'lowerBound': 1,
      'upperBound': 1
    };
    var appServerCapabilityElementType = {
      'type': 'xeditable',
      'lowerBound': 'xeditable',
      'upperBound': 'xeditable'
    };
    genericForm.addKeyToMap('capabilities', 'appServer', appServerCapability, {
      'type': true
    }, appServerCapabilityElementType);
    var instanceStates = ['notdeployed', 'stopped', 'started'];
    // Add some instance states
    genericForm.addElementsToArray('instanceStates', instanceStates, false, "xeditable");
    // Save
    genericForm.saveForm();
    expect(nodeTypeList.isDisplayed()).toBe(true);

    var firstNodeType = getFirstElementInNodeTypeList('fr.fastconnect.myCompute');
    firstNodeType.click();
    genericForm.expectValueFromPrimitive('id', 'fr.fastconnect.myCompute', "xeditable");
    genericForm.expectValueFromPrimitive('derivedFrom', 'fastconnect.nodes.Tomcat', "xeditable");
    genericForm.expectValueFromMap('properties', 'cpu', cpuProperty, cpuPropertyElementType);
    genericForm.expectValueFromMap('requirements', 'compute', computeRequirement, computeRequirementElementType);
    genericForm.expectValueFromMap('capabilities', 'appServer', appServerCapability, appServerCapabilityElementType);
    genericForm.expectValueFromArray('instanceStates', instanceStates, "xeditable");
    element(by.id('treeFormrootlabel')).click();

    //rename
    genericForm.sendValueToPrimitive('id', 'fr.fastconnect.myNewCompute', false, "xeditable");
    genericForm.saveForm();
    csarCommon.goToCsarSearchPage();
    csarCommon.goToCsarDetails(0);

    firstNodeType = getFirstElementInNodeTypeList('fr.fastconnect.myNewCompute');
    firstNodeType.click();
    genericForm.expectValueFromPrimitive('id', 'fr.fastconnect.myNewCompute', "xeditable");
    browser.element(by.binding('GENERIC_FORM.DELETE')).click();
    nodeTypeList = element(by.id('nodeTypeList'));
    expect(nodeTypeList.all(by.tagName('li')).count()).toEqual(0);
  });

  var getFirstElementInNodeTypeList = function(expectedId) {
    var nodeTypeList = element(by.id('nodeTypeList'));
    expect(nodeTypeList.all(by.tagName('li')).count()).toEqual(1);
    var firstNode = nodeTypeList.all(by.tagName('li')).first();
    expect(firstNode.element(by.tagName('h4')).getText()).toContain(expectedId);
    return firstNode;
  };
});
