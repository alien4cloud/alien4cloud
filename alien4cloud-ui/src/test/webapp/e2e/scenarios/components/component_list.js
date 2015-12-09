/* global by, element, describe, it, expect, browser, protractor */

'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var components = require('../../components/components');

function expectElementList() {
  expect(element(by.id('comp-search-side-panel')).isPresent()).toBe(true);
  expect(element(by.id('comp-search-result-panel')).isPresent()).toBe(true);
  var results = element.all(by.repeater('component in searchResult.data'));
  expect(results.count()).toEqual(20);
}

describe('Component List :', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('Admin should be able to see the upload box and element list', function(){
    components.go();
    expect(element(by.id('upload-csar')).isPresent()).toBe(true);
    expectElementList();
  });

  it('Component manager should be able to see the upload box and element list', function(){
    authentication.logout();
    authentication.login('componentManager');
    components.go();
    expect(element(by.id('upload-csar')).isPresent()).toBe(true);
    expectElementList();
  });

  it('Component browser should not be able to see the upload box and element list', function(){
    authentication.logout();
    authentication.login('componentBrowser');
    components.go();
    expect(element(by.id('upload-csar')).isPresent()).toBe(false);
    expectElementList();
  });

  it('Component browser should be able to list components and check pagination', function() {
    components.go();

    expect(element(by.id('comp-search-side-panel')).isPresent()).toBe(true);
    expect(element(by.id('comp-search-result-panel')).isPresent()).toBe(true);

    var results = element.all(by.repeater('component in searchResult.data'));
    expect(results.count()).toEqual(20);

    // pagination
    var pagination = element.all(by.repeater('page in pages'));
    expect(pagination.count()).toEqual(6); // First, Previous, 1, 2, Next, Last
    // go to the second page and check
    var secondPageElement = pagination.get(3);
    common.click(by.tagName('a'), secondPageElement);
    results = element.all(by.repeater('component in searchResult.data'));
    expect(results.count()).toEqual(10);
  });

  it('should be able to have components grouped by version when there are multiple versions of the same component', function() {
    components.go();
    var computeLine = common.element(by.id('li_tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT'));

    // version should be the latest
    var versionButton = common.element(by.id('tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT_versions'));
    expect(versionButton.getText()).toBe('1.0.0.wd06-SNAPSHOT');
    common.click(by.id('tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT_versions'));

    // assert that we have 2 versions
    var versions = computeLine.all(by.repeater('olderVersion in component.olderVersions'));
    expect(versions.count()).toEqual(2);

    // change version
    common.click(by.id('tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT_version_1.0.0.wd03-SNAPSHOT'));

    var oldVersionButton = common.element(by.id('tosca.nodes.Compute:1.0.0.wd03-SNAPSHOT_versions'));
    // assert the version is the right one
    expect(oldVersionButton.getText()).toBe('1.0.0.wd03-SNAPSHOT');
  });

  xit('should be able to use search to find components', function() {});
  
  it('afterAll', function() { authentication.logout(); });
});
