/* global element, by */

'use strict';

var path = require('path');

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var navigation = require('../common/navigation');
var topologyEditorCommon = require('../topology/topology_editor_common');
var componentData = require('../topology/component_data');

describe('Topology node template edition', function() {
  // this tests is processed as a suite and thus are dependent one from each others.
  var isBeforeAllDone = false;

  beforeEach(function() {
    if(isBeforeAllDone) {
      authentication.login('applicationManager');
      navigation.go('main', 'applications');
      browser.element(by.binding('application.name')).click();
      navigation.go('applications', 'topology');
    }
  });

  afterEach(function() {
    common.after();
  });

  it('beforeAll', function() {
    topologyEditorCommon.beforeTopologyTest();
    isBeforeAllDone = true;
  });

  it('should be able to see topology details', function() {
    console.log('################# should be able to see topology details');

    expect(element(by.id('topology')).isPresent()).toBe(true);
    expect(element(by.css('div.topology-details')).isPresent()).toBe(true);
  });

  it('should be able to add a node templates to a topology and see the node details', function() {
    console.log('################# should be able to add a node templates to a topology and see the node details');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({ war: componentData.fcTypes.war() });

    // should be display the link to the node detail
    expect(element(by.id('btn-tosca.nodes.Compute')).isPresent()).toBe(true);

    // test details visualisation
    var nodeToEdit = element(by.id('rect_Compute'));
    nodeToEdit.click();

    var nameSpan = element(by.css('div.topology-details h4 span[editable-text]'));
    expect(nameSpan.isDisplayed()).toBe(true);
  });

  it('should be able to edit a node template name', function() {
    var nodeToEdit = element(by.id('rect_Compute_2'));
    nodeToEdit.click();

    var nameSpan = element(by.css('div.topology-details h4 span[editable-text]'));

    var editForm;
    var editInput;
    // success update
    nameSpan.click();
    editForm = element(by.css('div.topology-details h4 form'));
    editInput = editForm.element(by.tagName('input'));
    editInput.clear();
    editInput.sendKeys('Compute_new_NAME');
    editForm.submit();
    browser.waitForAngular();
    expect(nameSpan.getText()).toContain('Compute_new_NAME');

    // fail update
    nameSpan.click();
    editForm = element(by.css('div.topology-details h4 form'));
    editInput = editForm.element(by.tagName('input'));
    editInput.clear();
    editInput.sendKeys('JavaRPM');
    editForm.submit();
    browser.waitForAngular();
    expect(nameSpan.getText()).toContain('Compute_new');
  });

  // TODO : randomly fails
  xit('should be able to edit a compute node template properties disk_size with constraint', function() {
    console.log('################# should be able to edit a compute node template properties disk_size with constraint');
    // Edit property disk_size with bad value
    var propertyName = 'disk_size';
    var diskSizeElement = element(by.id('p_' + propertyName));
    topologyEditorCommon.editNodeProperty('Compute', propertyName, 'E');
    browser.waitForAngular();

    // getting error div under the input
    topologyEditorCommon.checkPropertyEditionError('Compute', propertyName, '>');

    // Editing with a correct value
    topologyEditorCommon.editNodeProperty('Compute', propertyName, '50');
    browser.waitForAngular();

    // edition OK, no more <form>
    expect(diskSizeElement.isElementPresent(by.tagName('form'))).toBe(false);
  });

  // TODO : randomly fails
  xit('should be able to edit a JAVA node template properties version with constraint', function() {
    console.log('################# should be able to edit a JAVA node template properties version with constraint');

    var propertyName = 'version';
    var versionElement = element(by.id('p_' + propertyName));
    topologyEditorCommon.editNodeProperty('JavaRPM', propertyName, '1.2');
    browser.waitForAngular();

    topologyEditorCommon.checkPropertyEditionError('JavaRPM', propertyName, '>=');

    // Editing with a correct value
    topologyEditorCommon.editNodeProperty('JavaRPM', propertyName, '1.6');
    browser.waitForAngular();

    expect(versionElement.isElementPresent(by.tagName('form'))).toBe(false);
  });

  it('should be able to edit deployment artifact', function() {
    console.log('################# should be able to edit deployment artifact');

    var nodeToEdit = element(by.id('rect_War'));
    nodeToEdit.click();
    element.all(by.repeater('(artifactId, artifactDefinition) in selectedNodeTemplate.artifacts')).then(function(artifacts) {
      expect(artifacts.length).toEqual(1);
      var myWar = artifacts[0];
      expect(myWar.element(by.binding('artifactId')).getText()).toEqual('war');
      expect(myWar.element(by.binding('artifactDefinition.artifactType')).getText()).toEqual('tosca.artifacts.WarFile');
      expect(myWar.element(by.binding('artifactDefinition.artifactName')).getText()).toEqual('');
      var myWarUpdateButton = browser.element(by.css('input[type="file"]'));
      myWarUpdateButton.sendKeys(path.resolve(__dirname, '../../../../../alien4cloud-rest-it/src/test/resources/data/artifacts/myWar.war'));
      browser.waitForAngular();
      myWar.element(by.binding('artifactDefinition.artifactName')).getText().then(function(text) {
        expect(text.length).toBeGreaterThan(0);
      });
    });
  });


  it('should have the a todo list if topology is not valid', function() {
    console.log('################# should have the a todo list if topology is not valid');

    topologyEditorCommon.checkTodoList(true);
    topologyEditorCommon.removeNodeTemplate('Compute_new_NAME');
    topologyEditorCommon.removeNodeTemplate('War');
    topologyEditorCommon.checkTodoList(true);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.checkTodoList(true);
    topologyEditorCommon.editNodeProperty('Compute', 'os_arch', 'x86_64');
    topologyEditorCommon.editNodeProperty('Compute', 'os_type', 'windows');
    topologyEditorCommon.checkTodoList(false);
  });
});
