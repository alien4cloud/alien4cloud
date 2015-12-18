/* global describe, it, by, element */
'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var components = require('../../components/components');

describe('Component details', function() {
  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to set a component as recommended for a capability', function() {
    flagComponentAsRecommanded(blockStorageComponent, rootCapability);
    checkRecommanded(true, findCapabilityRow(rootCapability));
  });

  it('should be able to change the recommended component for a capability', function() {
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

  it('should be able to undefine a component as recommend for a capability', function() {
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

  it('afterAll', function() { authentication.logout(); });
});
