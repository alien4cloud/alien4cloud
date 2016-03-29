/* global describe, it, element, by, browser, expect */
'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var path = require('path');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var xedit = require('../../common/xedit');
var applications = require('../../applications/applications');

var csarsData = require(__dirname + '/_data/application_topology_suggestions_property/csars.json');
var applicationsData = require(__dirname + '/_data/application_topology_suggestions_property/applications.json');
var applicationversionData = require(__dirname + '/_data/application_topology_suggestions_property/applicationversions.json');
var applicationenvironmentsData = require(__dirname + '/_data/application_topology_suggestions_property/applicationenvironments.json');
var indexedartifacttypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexedartifacttypes.json');
var indexedcapabilitytypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexedcapabilitytypes.json');
var indexeddatatypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexeddatatypes.json');
var indexednodetypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexednodetypes.json');
var indexedrelationshiptypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexedrelationshiptypes.json');
var suggestionentryData = require(__dirname + '/_data/application_topology_suggestions_property/suggestionentry.json');
var topologiesData = require(__dirname + '/_data/application_topology_suggestions_property/topologies.json');

describe('Suggestion on property definition tests', function() {
  var appName = 'AlienUI-SuggestionEntry';

  var editNodePropertySuggestionAndCheck = function(nodeTemplateName, propertyName, propertyValue, componentType, isModalDisplay, save, selectUbuntuValue) {
    topologyEditorCommon.showComponentsTab();
    topologyEditorCommon.selectNodeAndGoToDetailBloc(nodeTemplateName, topologyEditorCommon.nodeDetailsBlocsIds[componentType]);
    var propertyElement = common.element(by.id(topologyEditorCommon.nodeDetailsBlocsIds[componentType] + '-panel'));
    propertyElement = common.element(by.id('p_' + propertyName), propertyElement);
    var editForm;

    common.click(by.css('span[editable-text]'), propertyElement);
    editForm = common.element(by.tagName('form'), propertyElement);
    var inputValue = common.element(by.tagName('input'), editForm);

    inputValue.clear();
    inputValue.sendKeys(propertyValue);
    expect(element.all(by.repeater('match in matches')).count()).toEqual(5);
    editForm.submit();

    expect(element(by.className('modal-dialog')).isPresent()).toBe(isModalDisplay);
    element(by.className('modal-dialog')).isPresent().then(function (isVisible) {
      if (isVisible) {
        if (selectUbuntuValue) {
          element(by.css('input[value="ubuntu"]')).click();
        }
        if (save) {
          common.click(by.id('btn-create'));
        } else {
          common.click(by.id('btn-cancel'));
        }
      }
    });
  };

  it('beforeAll', function() {
    setup.setup();
    setup.index('csar', 'csar', csarsData);
    setup.index('application', 'application', applicationsData);
    setup.index('applicationversion', 'applicationversion', applicationversionData);
    setup.index('applicationenvironment', 'applicationenvironment', applicationenvironmentsData);
    setup.index('toscaelement', 'indexedartifacttype', indexedartifacttypesData);
    setup.index('toscaelement', 'indexedcapabilitytype', indexedcapabilitytypesData);
    setup.index('toscaelement', 'indexeddatatype', indexeddatatypesData);
    setup.index('toscaelement', 'indexednodetype', indexednodetypesData);
    setup.index('toscaelement', 'indexedrelationshiptype', indexedrelationshiptypesData);
    setup.index('suggestionentry', 'suggestionentry', suggestionentryData);
    setup.index('topology', 'topology', topologiesData);
    common.home();
    authentication.login('admin');
  });

  it('should set the distribution to kubuntu and add it to the suggestionentry', function() {
    applications.goToApplicationTopologyPage(appName);
    editNodePropertySuggestionAndCheck('Compute', 'distribution', 'kubuntu', 'cap', true, true);
    xedit.expect('div_distribution', 'kubuntu');
  });

  it('should set the distribution to ubuntu, no modal should be present', function() {
    applications.goToApplicationTopologyPage(appName);
    editNodePropertySuggestionAndCheck('Compute', 'distribution', 'ubuntu', 'cap', false);
    xedit.expect('div_distribution', 'ubuntu');
  });

  it('should set the distribution to kubuntu, no modal should be present', function() {
    applications.goToApplicationTopologyPage(appName);
    editNodePropertySuggestionAndCheck('Compute', 'distribution', 'kubuntu', 'cap', false);
    xedit.expect('div_distribution', 'kubuntu');
  });

  it('should set the distribution to lubuntu and select the selection with ubuntu', function() {
    applications.goToApplicationTopologyPage(appName);
    editNodePropertySuggestionAndCheck('Compute', 'distribution', 'lubuntu', 'cap', true, true, true);
    xedit.expect('div_distribution', 'ubuntu');
  });

  it('should set the distribution to lubuntu and cancel the modal', function() {
    applications.goToApplicationTopologyPage(appName);
    editNodePropertySuggestionAndCheck('Compute', 'distribution', 'lubuntu', 'cap', true, false);
    topologyEditorCommon.checkPropertyEditionError('Compute', 'distribution', 'Cancelled');
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
