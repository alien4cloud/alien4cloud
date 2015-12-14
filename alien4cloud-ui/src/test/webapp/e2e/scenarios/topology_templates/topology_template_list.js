/* global describe, it, element, by, expect */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var topologyTemplates = require('../../topology_templates/topology_templates');

var topologyTemplateName = 'MyTopologyTemplate';

describe('Topology templates list:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('architect');
  });

  it('Architect should be able to add a new topology template', function() {
    topologyTemplates.create(topologyTemplateName, 'description');
    toaster.expectNoErrors();
    topologyTemplates.checkTopologyTemplate(topologyTemplateName);
  });

  it('Architect should not be able to add a new topology template with an existing name', function() {
    topologyTemplates.create(topologyTemplateName, 'description');
    toaster.expectErrors();
    toaster.dismissIfPresent();
  });

  it('Architect should be able to cancel creation of a new topology template', function() {
    topologyTemplates.create(topologyTemplateName, 'description', true);
    toaster.expectNoErrors();
  });

  it('Architect should be able to delete a topology template', function() {
    topologyTemplates.go();
    topologyTemplates.searchTopologyTemplate(topologyTemplateName);
    common.deleteWithConfirm('delete-template_' + topologyTemplateName, true);
    toaster.expectNoErrors();
  });

  it('Admin should be able to see topology template list and check pagination', function() {
    authentication.logout();
    authentication.login('admin');
    topologyTemplates.go();
    expect(element(by.id('btn-add-template')).isPresent()).toBe(true);
    var pages = element.all(by.repeater('page in pages'));
    expect(pages.count()).toBe(7);
  });

  it('Component manager should not be able to see topology template list', function() {
    authentication.logout();
    authentication.login('componentManager');
    expect(element(by.id('menu.topologytemplates')).isPresent()).toBe(false);
  });

  it('afterAll', function() { authentication.logout(); });
});
