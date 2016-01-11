/* global element, by */

'use strict';

var path = require('path');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var xEdit = require('../../common/xedit');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var componentData = require('../../topology/component_data');

describe('Topology node template edition :', function() {

  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('applicationManager');
    common.go('main', 'applications');
    browser.element(by.binding('application.name')).click();
    common.go('applications', 'topology');
  });

  it('should be able to see topology details', function() {
    console.log('################# should be able to see topology details');

    expect(element(by.id('topology')).isPresent()).toBe(true);
    expect(element(by.id('nodetemplate-details')).isPresent()).toBe(true);
  });

  it('should be able to add a node templates to a topology and see the node details', function() {
    console.log('################# should be able to add a node templates to a topology and see the node details');

    topologyEditorCommon.addNodeTemplatesCenterAndZoom(componentData.simpleTopology.nodes);
    topologyEditorCommon.addNodeTemplatesCenterAndZoom({
      tomcat: componentData.alienTypes.tomcat(),
      war: componentData.alienTypes.war()
    });

    topologyEditorCommon.searchComponents('tosca.nodes.Compute');
    // should be display the link to the node detail
    expect(element(by.id('btn-tosca.nodes.Compute')).isPresent()).toBe(true);

    // test details visualisation
    var nodeToEdit = element(by.id('rect_Compute'));
    nodeToEdit.click();

    var nameSpan = element(by.id('nodetemplate-details')).element(by.css('h3 span[editable-text]'));
    expect(nameSpan.isDisplayed()).toBe(true);
  });

  it('should be able to edit a node template name', function() {
    console.log('################# should be able to edit a node template name');
    element(by.id('rect_Compute-2')).click();
    // success update
    xEdit.sendKeys('nodetemplate-titles', 'Compute-new-NAME');
    xEdit.expect('nodetemplate-titles', 'Compute-new-NAME');
    // fail update
    xEdit.sendKeys('nodetemplate-titles', 'Java');
    toaster.expectErrors();
    toaster.dismissIfPresent();
    xEdit.expect('nodetemplate-titles', 'Compute-new-NAME');
  });

  it('should be able to edit a scalar-unit.size and time', function() {
    console.log('################# should be able to edit a scalar-unit.size and time');
    var diskSizeName = 'disk_size';
    var diskSizeElement = element(by.id('p_' + diskSizeName));
    topologyEditorCommon.editNodeProperty('Compute', diskSizeName, '-1', 'cap', 'MB');
    topologyEditorCommon.checkPropertyEditionError('Compute', diskSizeName, '>');
    // Editing with a correct value
    topologyEditorCommon.editNodeProperty('Compute', diskSizeName, '2', 'cap', 'GB');
    expect(diskSizeElement.isElementPresent(by.tagName('form'))).toBe(false);

    var cpuFrequencyName = 'cpu_frequency';
    var diskAccessTimeElement = element(by.id('p_' + cpuFrequencyName));
    topologyEditorCommon.editNodeProperty('Compute', cpuFrequencyName, '-1', 'cap', 'Hz');
    topologyEditorCommon.checkPropertyEditionError('Compute', cpuFrequencyName, '>');
    // Editing with a correct value
    topologyEditorCommon.editNodeProperty('Compute', cpuFrequencyName, '4', 'cap', 'GHz');
    expect(diskAccessTimeElement.isElementPresent(by.tagName('form'))).toBe(false);
  });

  it('should be able to edit a Compute node template properties version with constraint', function() {
    console.log('################# should be able to edit a JAVA node template properties version with constraint');
    var propertyName = 'component_version';
    var versionElement = element(by.id('p_' + propertyName));
    topologyEditorCommon.editNodeProperty('Java', propertyName, 'ABC');
    topologyEditorCommon.checkPropertyEditionError('Java', propertyName, 'is not version');
    // Editing with a correct value
    topologyEditorCommon.editNodeProperty('Java', propertyName, '1.8');
    expect(versionElement.isElementPresent(by.tagName('form'))).toBe(false);
  });

  it('should be able to edit deployment artifact', function() {
    console.log('################# should be able to edit deployment artifact');
    topologyEditorCommon.selectNodeAndGoToDetailBloc('War', topologyEditorCommon.nodeDetailsBlocsIds['art']);
    element.all(by.repeater('(artifactId, artifact) in selectedNodeTemplate.artifacts')).then(function(artifacts) {
      expect(artifacts.length).toEqual(1);
      var myWar = artifacts[0];
      expect(myWar.element(by.binding('artifactId')).getText()).toEqual('war_file');
      expect(myWar.element(by.binding('artifact.artifactType')).getText()).toEqual('alien.artifacts.WarFile');
      expect(myWar.element(by.binding('artifact.artifactName')).getText()).toEqual('myWar.war');
      var myWarUpdateButton = browser.element(by.css('input[type="file"]'));
      myWarUpdateButton.sendKeys(path.resolve(__dirname,
        '../../../../../../../alien4cloud-rest-it/src/test/resources/data/artifacts/myWar.war'));
      common.element(by.binding('artifact.artifactName'), myWar).getText().then(function(text) {
        expect(text.length).toBeGreaterThan(0);
      });
    });
  });

  it('should have the a todo list if topology is not valid', function() {
    console.log('################# should have the a todo list if topology is not valid');
    topologyEditorCommon.checkTodoList(true);
    topologyEditorCommon.removeNodeTemplate('Compute-new-NAME');
    topologyEditorCommon.removeNodeTemplate('War');
    topologyEditorCommon.removeNodeTemplate('Tomcat');
    topologyEditorCommon.checkTodoList(true);
    topologyEditorCommon.addRelationship(componentData.simpleTopology.relationships.hostedOnCompute);
    topologyEditorCommon.checkTodoList(false);
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
