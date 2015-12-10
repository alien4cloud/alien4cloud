/* global describe, it, element, by, expect */
'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var topologyTemplates = require('../../topology_templates/topology_templates');

var topologyTemplateName = 'MyTopologyTemplate';

describe('Topology templates list:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('architect');
  });

  it('should be able to add a new topology template', function() {
    authentication.logout();
    authentication.login('architect');
    topologyTemplates.create(topologyTemplateName, 'description');
    topologyTemplates.checkTopologyTemplate(topologyTemplateName);

    topologyTemplates.go();
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('should not be able to add a new topology template with an existing name', function() {
    topologyTemplates.create(topologyTemplateName, 'description');
    common.dismissAlertIfPresent();
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('should be able to cancel creation of a new topology template', function() {
    topologyTemplates.create('name', 'description', true);
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(1);
  });

  it('should be able to delete a topology template', function() {
    topologyTemplates.go();
    common.deleteWithConfirm('delete-template_' + topologyTemplateName, true);
    var templates = element.all(by.repeater('topologyTemplate in searchResult.data.data'));
    expect(templates.count()).toBe(0);
  });

  it('Admin should be able to see topology template list', function() {
    authentication.logout();
    authentication.login('admin');
    topologyTemplates.go();
    expect(element(by.id('btn-add-template')).isPresent()).toBe(true);
  });

  it('Component manager should not be able to see topology template list', function() {
    authentication.logout();
    authentication.login('componentManager');
    expect(element(by.id('menu.topologytemplates')).isPresent()).toBe(false);
  });

  it('afterAll', function() { authentication.logout(); });
});
