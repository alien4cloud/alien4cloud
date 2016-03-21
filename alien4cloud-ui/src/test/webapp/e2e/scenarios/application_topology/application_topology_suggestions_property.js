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
var applicationenvironmentsData = require(__dirname + '/_data/application_topology_suggestions_property/applicationenvironments.json');
var applicationsData = require(__dirname + '/_data/application_topology_suggestions_property/applications.json');
var indexedartifacttypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexedartifacttypes.json');
var indexedcapabilitytypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexedcapabilitytypes.json');
var indexeddatatypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexeddatatypes.json');
var indexednodetypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexednodetypes.json');
var indexedrelationshiptypesData = require(__dirname + '/_data/application_topology_suggestions_property/indexedrelationshiptypes.json');
var suggestionentryData = require(__dirname + '/_data/application_topology_suggestions_property/suggestionentry.json');

describe('Nodetemplate properties and artifact reset to default value', function() {
  var appName = 'AlienUI-SuggestionEntry';

  // it('beforeAll', function() {
  //   setup.setup();
  //   setup.index('csar', 'csar', csarsData);
  //   setup.index('applicationenvironment', 'applicationenvironment', applicationenvironmentsData);
  //   setup.index('application', 'application', applicationsData);
  //   setup.index('toscaelement', 'indexedartifacttype', indexedartifacttypesData);
  //   setup.index('toscaelement', 'indexedcapabilitytype', indexedcapabilitytypesData);
  //   setup.index('toscaelement', 'indexeddatatype', indexeddatatypesData);
  //   setup.index('toscaelement', 'indexednodetype', indexednodetypesData);
  //   setup.index('toscaelement', 'indexedrelationshiptype', indexedrelationshiptypesData);
  //   setup.index('suggestionentry', 'suggestionentry', suggestionentryData);
  //   common.home();
  //   authentication.login('applicationManager');
  // });

  it('should add a compute with the distribution kubuntu and add it to the suggestionentry', function() {
    applications.goToApplicationTopologyPage(appName);
    topologyEditorCommon.selectNodeAndGoToDetailBloc('Compute', topologyEditorCommon.nodeDetailsBlocsIds.pro);
    topologyEditorCommon.editNodeProperty('Compute', 'distribution', 'kubuntu');
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
