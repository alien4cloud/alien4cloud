/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var navigation = require('../../common/navigation');
var components = require('../../components/components');

var blockStorageComponent = {
  id: 'tosca.nodes.BlockStorage:2.0',
  elementId: 'tosca.nodes.BlockStorage',
  archiveVersion: '2.0'
};
var computeComponent = {
  id: 'tosca.nodes.Compute:1.0.0.wd03-SNAPSHOT',
  elementId: 'tosca.nodes.Compute',
  archiveVersion: '1.0'
};
var computeComponentV2 = {
  id: 'tosca.nodes.Compute:2.0',
  elementId: 'tosca.nodes.Compute',
  archiveVersion: '2.0'
};
var rootCapability = 'feature';
var rootCapabilityType = 'tosca.capabilities.Feature';
var computeCapability = 'compute';

var checkRecommanded = function(recommended, capabilityRow) {
  expect(capabilityRow.element(by.css('.alert-success')).isPresent()).toBe(recommended);
  expect(capabilityRow.element(by.css('a.btn-success')).isDisplayed()).toBe(!recommended);
  expect(capabilityRow.element(by.css('a.btn-danger')).isDisplayed()).toBe(recommended);
};

var findCapabilityRow = function(testedCapabilityId) {
  return browser.element(by.id(testedCapabilityId));
};

var flagComponentAsRecommanded = function(component, testedCapability) {
  components.goToComponentDetailPage(component.id);
  expect(element.all(by.binding('component.elementId')).first().getText()).toContain(component.elementId);
  expect(element(by.binding('component.archiveVersion')).getText()).toContain(component.archiveVersion);

  var firstCapaRow = findCapabilityRow(testedCapability);
  expect(firstCapaRow.getText()).toContain(testedCapability);
  expect(firstCapaRow.isElementPresent(by.css('a.btn-success'))).toBe(true);
  checkRecommanded(false, firstCapaRow);
  var recommendButton = firstCapaRow.element(by.css('a.btn-success'));
  // recommend for this capability
  recommendButton.click();
};

describe('Component Details :', function() {
  // Load up a view and wait for it to be done with its rendering and epicycles.
  beforeEach(function() {
    common.before();
    // Login
    authentication.login('componentManager');
    navigation.go('main', 'components');
  });

  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to go to component search page, upload a csar, list components and check pagination', function() {
    console.log('################# should be able to go to component search page, upload a csar, list components and check pagination');
    // the serach side panel
    expect(element(by.id('comp-search-side-pannel')).isPresent()).toBe(true);
    // the search result panel
    expect(element(by.id('comp-search-result-panel')).isPresent()).toBe(true);

    var results = element.all(by.repeater('component in searchResult.data.data'));
    expect(results.count()).toEqual(20);

    // pagination
    var pagination = element.all(by.repeater('page in pages'));
    expect(pagination.count()).toEqual(6);

    // go to the second page and check
    pagination.get(3).element(by.tagName('a')).click();
    results = element.all(by.repeater('component in searchResult.data.data'));
    expect(results.count()).toEqual(7);
  });

  it('should be able to have components grouped by version when there are multiple versions of the same component', function() {
    console.log('################# should be able to have components grouped by version when there are multiple versions of the same component');
    var results = element.all(by.repeater('component in searchResult.data.data'));
    expect(results.count()).toEqual(20);

    // pagination
    var pagination = element.all(by.repeater('page in pages'));
    expect(pagination.count()).toEqual(6);

    // go to the second page and check
    pagination.get(3).element(by.tagName('a')).click();
    results = element.all(by.repeater('component in searchResult.data.data'));
    expect(results.count()).toEqual(7);
    pagination.get(0).element(by.tagName('a')).click();
    browser.waitForAngular();
    components.goToComponentDetailPage(computeComponentV2.id);
    expect(element.all(by.binding('component.elementId')).first().getText()).toContain(computeComponentV2.elementId);
    expect(element(by.binding('component.archiveVersion')).getText()).toContain(computeComponentV2.archiveVersion);

    navigation.go('main', 'components');
    components.changeComponentVersionAndGo(computeComponentV2.id, computeComponent.archiveVersion);
    expect(element.all(by.binding('component.elementId')).first().getText()).toContain(computeComponent.elementId);
    expect(element(by.binding('component.archiveVersion')).getText()).toContain(computeComponent.archiveVersion);
  });

  it('should be able to see details of the component [' + blockStorageComponent.id + '], and recommend that component as default for a capability ', function() {
    console.log('################# should be able to see details of the component [' + blockStorageComponent.id + '], and recommend that component as default for a capability ');
    flagComponentAsRecommanded(blockStorageComponent, rootCapability);
    checkRecommanded(true, findCapabilityRow(rootCapability));
  });

  it('should be able to trigger recommendation of another component [' + blockStorageComponent.id + '] as default for an already recommended capability: cancel and confirm ', function() {
    console.log('################# should be able to trigger recommendation of another component [' + blockStorageComponent.id   + '] as default for an already recommended capability: cancel and confirm ');
    navigation.go('main', 'components');
    var pagination = element.all(by.repeater('page in pages'));
    pagination.last().element(by.tagName('a')).click();
    components.goToComponentDetailPage(blockStorageComponent.id);
    checkRecommanded(true, findCapabilityRow(rootCapability));

    components.goToComponentDetailPage(computeComponentV2.id);
    expect(element.all(by.binding('component.elementId')).first().getText()).toContain(computeComponentV2.elementId);

    // first be sure it is not recommended yet
    var firstCapaRow = findCapabilityRow(rootCapability);
    expect(firstCapaRow.getText()).toContain(rootCapability);
    checkRecommanded(false, firstCapaRow);

    var recommendButton = firstCapaRow.element(by.css('a.btn-success'));

    // case cancel
    // trigger for recommendation
    recommendButton.click();
    browser.sleep(1000); // DO NOT REMOVE
    expect(element(by.className('modal-body')).getText()).toContain(blockStorageComponent.id);
    expect(element(by.className('modal-body')).getText()).toContain(rootCapabilityType);
    element(by.binding('CANCEL')).click();
    checkRecommanded(false, firstCapaRow);

    // case confirm
    // trigger for recommendation
    recommendButton.click();
    expect(element(by.className('modal-body')).getText()).toContain(blockStorageComponent.id);
    expect(element(by.className('modal-body')).getText()).toContain(rootCapabilityType);
    element(by.binding('COMPONENTS.CONFIRM_RECOMMENDATION_MODAL.OK')).click();
    checkRecommanded(true, findCapabilityRow(rootCapability));
  });

  it('should be able to undefine the component [' + computeComponentV2.id + '], as default for a capability ', function() {
    console.log('################# should be able to undefine the component [' + computeComponentV2.id + '], as default for a capability ');
    navigation.go('main', 'components');
    var pagination = element.all(by.repeater('page in pages'));
    browser.actions().click(pagination.last().element(by.tagName('a'))).perform();
    components.goToComponentDetailPage(computeComponentV2.id);

    expect(element.all(by.binding('component.elementId')).first().getText()).toContain(computeComponentV2.elementId);

    flagComponentAsRecommanded(computeComponentV2, computeCapability);
    // first be sure it is already recommended
    var firstCapaRow = findCapabilityRow(computeCapability);
    expect(firstCapaRow.getText()).toContain(computeCapability);
    expect(firstCapaRow.isElementPresent(by.css('a.btn-danger'))).toBe(true);
    checkRecommanded(true, findCapabilityRow(computeCapability));
    var undefinedDefaultButton = firstCapaRow.element(by.css('a.btn-danger'));

    // undefine the component as default
    undefinedDefaultButton.click();
    checkRecommanded(false, findCapabilityRow(computeCapability));
  });

});
