/* global describe, it, element, by, browser, expect */
'use strict';

var common = require('../../common/common');
var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var path = require('path');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var xedit = require('../../common/xedit');
var applications = require('../../applications/applications');

var topologiesData = require(__dirname + '/_data/application_topology_editor_reset/topologies.json');
var topologytemplatesData = require(__dirname + '/_data/application_topology_editor_reset/topologytemplates.json');
var applicationenvironmentsData = require(__dirname + '/_data/application_topology_editor_reset/applicationenvironments.json');
var applicationsData = require(__dirname + '/_data/application_topology_editor_reset/applications.json');
var applicationversionsData = require(__dirname + '/_data/application_topology_editor_reset/applicationversions.json');

describe('Nodetemplate properties and artifact reset to default value', function() {
  var reloadTopopology = function reloadTopopology() {
    common.go('applications', 'deployment');
    common.go('applications', 'topology');
  };

  it('beforeAll', function() {
    setup.setup();
    setup.index('topology', 'topology', topologiesData);
    setup.index('topologytemplate', 'topologytemplate', topologytemplatesData);
    setup.index('application', 'application', applicationsData);
    setup.index('applicationenvironment', 'applicationenvironment', applicationenvironmentsData);
    setup.index('applicationversion', 'applicationversion', applicationversionsData);
    common.home();
    authentication.login('applicationManager');
  });

  it('should be able to change a property for a node and reset to the default value', function() {
    var memSizeId = 'mem_size';
    var computeNameId = 'rect_Compute';
    applications.goToApplicationDetailPage('AlienUITAppTopoEdit1');
    topologyEditorCommon.go();

    // change properties on node compute
    common.click(by.id(computeNameId));
    xedit.expect('p_' + memSizeId, '2048');
    topologyEditorCommon.editNodeProperty('Compute', memSizeId, '16000', 'cap', 'GB');

    reloadTopopology();

    // reset this mem property
    common.click(by.id(computeNameId));
    xedit.expect('p_' + memSizeId, '16000');
    common.click(by.id('reset-property-' + memSizeId));

    reloadTopopology();

    // check the supposed reseted mem_size
    common.click(by.id(computeNameId));
    xedit.expect('p_' + memSizeId, '');
  });

  it('should be able to change deployment artifact for a node and reset to the default value', function() {
    applications.goToApplicationDetailPage('AlienUITAppTopoEdit2');
    topologyEditorCommon.go();
    topologyEditorCommon.selectNodeAndGoToDetailBloc('War', topologyEditorCommon.nodeDetailsBlocsIds['art']);
    element.all(by.repeater('(artifactId, artifact) in selectedNodeTemplate.artifacts')).then(function(artifacts) {
      // check default value
      expect(artifacts.length).toEqual(1);
      var myScript = artifacts[0];
      expect(myScript.element(by.binding('artifactId')).getText()).toEqual('war_file');
      expect(myScript.element(by.binding('artifact.artifactType')).getText()).toEqual('alien.artifacts.WarFile');
      expect(element(by.id('war_file-artifactName')).getText()).toContain('warFiles');
      //expect(element(by.id('span-artifactName_warFiles/helloWorld.war')).isPresent()).toBe(true);

      // update the war
      var myScriptUpdateButton = browser.element(by.css('input[type="file"]'));
      myScriptUpdateButton.sendKeys(path.resolve(__dirname,
        '../../../../../../../alien4cloud-rest-it/src/test/resources/data/artifacts/myWar.war'));
      browser.waitForAngular();
      expect(element(by.id('war_file-artifactName')).getText()).toContain('myWar');

      // reset check the artifact name is back
      common.click(by.id('reset-artifact-war_file'));
      expect(myScript.element(by.binding('artifactId')).getText()).toEqual('war_file');
      expect(myScript.element(by.binding('artifact.artifactType')).getText()).toEqual('alien.artifacts.WarFile');
      expect(element(by.id('war_file-artifactName')).getText()).toContain('warFiles');
    });
  });

  it('afterAll', function() {
    authentication.logout();
  });
});
