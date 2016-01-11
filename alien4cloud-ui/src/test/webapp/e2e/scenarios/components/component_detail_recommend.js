/* global describe, it, by, element */
'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var components = require('../../components/components');

var featureCapability = 'feature';
var featureCapabilityType = 'tosca.capabilities.Node';
var computeCapability = 'compute';

var storage = {
    type:'tosca.nodes.BlockStorage',
    version:'1.0.0.wd06-SNAPSHOT',
    id:function(){
      return this.type+':'+this.version;
    }
}
var compute = {
    type:'tosca.nodes.Compute',
    version:'1.0.0.wd06-SNAPSHOT',
    id:function(){
      return this.type+':'+this.version;
    }
}

var checkRecommanded = function(recommended, capabilityId) {
  var capabilityRow = common.element(by.id(capabilityId));
  expect(capabilityRow.element(by.css('.alert-success')).isPresent()).toBe(recommended);
  expect(element(by.id(capabilityId+'_unflagRecommended')).isPresent()).toBe(recommended);
  expect(element(by.id(capabilityId+'_flagRecommended')).isPresent()).toBe(!recommended);
};


var flagComponentAsRecommanded = function(capabilityId) {
  var capabilityDisplayRow = common.element(by.id(capabilityId));
  expect(capabilityDisplayRow.all(by.tagName('td')).first().getText()).toEqual(capabilityId);
  checkRecommanded(false, capabilityId);
  // recommend for this capability
  common.click(by.id(capabilityId+'_flagRecommended'));
};

var unflagComponentAsRecommended = function(capabilityId) {
  var capabilityDisplayRow = common.element(by.id(capabilityId));
  expect(capabilityDisplayRow.all(by.tagName('td')).first().getText()).toEqual(capabilityId);
  checkRecommanded(true, capabilityId);
  // recommend for this capability
  common.click(by.id(capabilityId+'_unflagRecommended'));
}

describe('Component details recommend component for capabilities', function() {
  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
    components.go();
  });

  it('should be able to set a component as recommended for a capability', function() {
    components.search(storage.type);
    common.click(by.id('li_'+storage.id()));
    flagComponentAsRecommanded(featureCapability);
    checkRecommanded(true, featureCapability);
  });

  it('should be able to change the recommended component for a capability', function() {
    components.go()
    components.search(compute.type);
    common.click(by.id('li_'+compute.id()));

    // first be sure it is not recommended yet
    checkRecommanded(false, featureCapability);

    // case cancel
    // trigger for recommendation
    flagComponentAsRecommanded(featureCapability);
    browser.sleep(1000); // DO NOT REMOVE
    expect(element(by.className('modal-body')).getText()).toContain(storage.id());
    expect(element(by.className('modal-body')).getText()).toContain(featureCapabilityType);
    element(by.binding('CANCEL')).click();
    checkRecommanded(false, featureCapability);

    // case confirm
    // trigger for recommendation
    flagComponentAsRecommanded(featureCapability);
    expect(element(by.className('modal-body')).getText()).toContain(storage.id());
    expect(element(by.className('modal-body')).getText()).toContain(featureCapabilityType);
    element(by.binding('COMPONENTS.CONFIRM_RECOMMENDATION_MODAL.OK')).click();
    checkRecommanded(true, featureCapability);
  });

  it('should be able to undefine a component as recommend for a capability', function() {
    // undefine the component as default
    unflagComponentAsRecommended(featureCapability);
    checkRecommanded(false, featureCapability);
  });

  it('afterAll', function() { authentication.logout(); });
});
