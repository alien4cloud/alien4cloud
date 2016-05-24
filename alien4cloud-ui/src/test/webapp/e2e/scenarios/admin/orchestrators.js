/* global by, element, describe, it, expect, browser, protractor */

'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var orchestrators = require('../../admin/orchestrators');

describe('Orchestrators management', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to create an orchestrator', function() {
    orchestrators.go();
    var rows = element.all(by.repeater('orchestrator in orchestrators'));
    expect(rows.count()).toEqual(2);
    orchestrators.create('orc');
    // Check that you have a 3 lines with status disabled, name and plugin of the orc.
    rows = element.all(by.repeater('orchestrator in orchestrators'));
    expect(rows.count()).toEqual(3);
    rows.then( function(rowArray){
      var newRowElement = rowArray[2];
      var stateEl = common.element(by.className('text-muted'), newRowElement);
      var nameEl = common.element(by.binding('orchestrator.name'), newRowElement);
      var pluginIdEl = common.element(by.binding('orchestrator.pluginId'), newRowElement);
      expect(stateEl.isPresent()).toBe(true);
      expect(nameEl.getText()).toBe('orc');
      expect(pluginIdEl.getText()).toBe('alien4cloud-mock-paas-provider:1.0');
    });
  });

  it('should be able to cancel orchestrator creation', function() {
    orchestrators.go();
    var rows = element.all(by.repeater('orchestrator in orchestrators'));
    rows.count().then(function(count) {
      // start to create but click on cancel.
      orchestrators.create('orc', true);
      // check that we still have the same number of orchestrators
      rows = element.all(by.repeater('orchestrator in orchestrators'));
      expect(rows.count()).toEqual(count);
    });
  });

  it('should fail to create an orchestrator with existing name', function() {
    orchestrators.go();
    var rows = element.all(by.repeater('orchestrator in orchestrators'));
    rows.count().then(function(count) {
      orchestrators.create('Mock Orchestrator Development');
      expect(common.element(by.binding('toaster.html')).getText()).toBe('Object already exists\nThe posted object already exist.');
      // check that we still have the same number of orchestrators
      rows = element.all(by.repeater('orchestrator in orchestrators'));
      expect(rows.count()).toEqual(count);
    });
  });

  it('should be able to rename an orchestrator', function() {
    orchestrators.go();
    common.click(by.id('orchestrator_f3657e4d-4250-45b4-a862-2e91699ef7a1'));
    xedit.sendKeys('orchestrator-name', 'Mock Orchestrator Development Renamed');
    var orchestratorNameEl = common.element(by.id('orchestrator-name'));
    expect(orchestratorNameEl.getText()).toBe('Mock Orchestrator Development Renamed');
    xedit.sendKeys('orchestrator-name', 'Mock Orchestrator Development');
    expect(orchestratorNameEl.getText()).toBe('Mock Orchestrator Development');
  });

  it('should fail to rename an orchestrator with existing name', function() {
    orchestrators.go();
    common.click(by.id('orchestrator_f3657e4d-4250-45b4-a862-2e91699ef7a1'));
    xedit.sendKeys('orchestrator-name', 'Mock Orchestrator Production');
    expect(common.element(by.binding('toaster.html')).getText()).toBe('Object already exists\nThe posted object already exist.');
    browser.actions().sendKeys(protractor.Key.ESCAPE).perform();
    var orchestratorNameEl = common.element(by.id('orchestrator-name'));
    expect(orchestratorNameEl.getText()).toBe('Mock Orchestrator Development');
  });

  it('should be able to configure a disabled orchestrator', function() {
    // ensure that we have the orc orchestrator and it is disabled
    //TODO
    //
  });

  it('should be able to configure an enabled orchestrator', function() {
    orchestrators.go();
    common.click(by.id('orchestrator_f3657e4d-4250-45b4-a862-2e91699ef7a1'));
    var stateDiv = common.element(by.id('orchestrator-state'));
    var stateIcon = stateDiv.element(by.className('text-success'));
    expect(stateIcon.isPresent()).toBe(true); // assert that the orchestrator is enabled
    common.click(by.id('menu.orchestrators.configuration')); // go to configuration page
    // expect the configuration element to be disabled
    // click on unlock
    common.click(by.id('orchestrator-configuration-unlock-btn'));
    // expect the configuration element to be available
  });

  it('should be able to configure an orchestrator deployment naming policy', function() {
    //TODO
  });

  it('should be able to delete an orchestrator with no deployments', function() {
    //TODO
  });

  it('should fail to delete an orchestrator with deployments', function() {
    //TODO
  });

  it('should be able to disable an orchestrator with no deployments', function() {
    //TODO
  });

  it('should not be able to disable an orchestrator with deployments', function() {
    //TODO
  });

  it('afterAll', function() { authentication.logout(); });
});
