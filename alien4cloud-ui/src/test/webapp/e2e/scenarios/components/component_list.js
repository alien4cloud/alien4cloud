/* global by, element, describe, it, expect, browser, protractor */

'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var components = require('../../components/components');

describe('Component List :', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to list components and check pagination', function() {
    components.go();

    expect(element(by.id('comp-search-side-panel')).isPresent()).toBe(true);
    expect(element(by.id('comp-search-result-panel')).isPresent()).toBe(true);

    var results = element.all(by.repeater('component in searchResult.data'));
    // expect(results.count()).toEqual(20);
    expect(results.count()).toEqual(17);

    // pagination
    var pagination = element.all(by.repeater('page in pages'));
    expect(pagination.count()).toEqual(5);

    // go to the second page and check
    //pagination.get(3).element(by.tagName('a')).click();
    // results = element.all(by.repeater('component in searchResult.data'));
    // expect(results.count()).toEqual(7);
  });

  xit('should be able to have components grouped by version when there are multiple versions of the same component', function() {
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

  xit('should be able to use search to find components', function() {
  });

  it('afterAll', function() { authentication.logout(); });
});
