/* global describe, it, element, by, expect */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var genericForm = require('../../generic_form/generic_form');
var tagConfigCommon = require('../../admin/metaprops_configuration_common');
var orchestrators = require('../../admin/orchestrators');

describe('Meta properties configuration', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to add/edit/delete a meta properties configuration', function() {
    var newName = 'NewName';
    tagConfigCommon.addTagConfiguration(tagConfigCommon.maturityTag, tagConfigCommon.tagMaturityValidValuesConstraint);

    tagConfigCommon.clickFirstElementInTagList(tagConfigCommon.maturityTag.name.value);
    common.click(by.id('breadcrumbrootlabel'));

    // rename
    genericForm.sendValueToPrimitive('name', newName, false, 'xeditable');
    common.click(by.id('closeGenericFormButton'));

    // delete
    tagConfigCommon.clickFirstElementInTagList(newName);
    genericForm.expectValueFromPrimitive('name', newName, 'xeditable');
    common.click(by.id('deleteGenericFormButton'));

    // count tags array
    var tagConfigurationsTable = common.element(by.id('tagConfigurationsTable'));
    expect(tagConfigurationsTable.all(by.tagName('tr')).count()).toEqual(0);
  });

  it('should add 2 configuration meta properties and check meta property configurations table', function() {
    // add tags
    tagConfigCommon.createConfigurationTags();

    // count tags array
    var tagConfigurationsTable = common.element(by.id('tagConfigurationsTable'));
    expect(tagConfigurationsTable.all(by.tagName('tr')).count()).toEqual(2);
  });

  it('should add a location meta-property and set <success> as value', function() {
    orchestrators.go();
    common.click(by.id('orchestrator_f3657e4d-4250-45b4-a862-2e91699ef7a1'));
    common.click(by.id('menu.orchestrators.locations'));
    expect(element(by.id('cloudMetaPropertiesDisplay')).isPresent()).toBe(false);
    tagConfigCommon.addTagConfiguration(tagConfigCommon.defaultLocationProperty, null);
  });

  it('afterAll', function() { authentication.logout(); });
});
