/* global describe, it, element, by, expect, browser */
'use strict';

var setup = require('../../common/setup');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var topologyTemplates = require('../../topology_templates/topology_templates');

var topologyTemplateName = 'MyTopologyTemplate';
var topologyTemplateNewName = 'MyNewTopologyTemplate';

describe('Topology templates details:', function() {
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('architect');
    topologyTemplates.create(topologyTemplateName, 'description');
  });

  it('Architect should be able to rename a topology template', function() {
    topologyTemplates.goToTopologyTemplateDetails(topologyTemplateName);
    xedit.sendKeys('template_' + topologyTemplateName + '_name', topologyTemplateNewName, false);
    expect(element(by.id('template_' + topologyTemplateNewName + '_name')).isPresent()).toBe(true);
  });

  it('Architect should not be able to rename a topology template with an existing name', function() {
    topologyTemplates.create(topologyTemplateName, 'description');
    topologyTemplates.goToTopologyTemplateDetails(topologyTemplateNewName);
    xedit.sendKeys('template_' + topologyTemplateNewName + '_name', topologyTemplateName, false);
    toaster.expectErrors();
    element(by.css('#template_' + topologyTemplateNewName + '_name input')).sendKeys('0');
    toaster.dismissIfPresent();
  });

  it('Architect should be able to edit the topology template description', function() {
    var newDescription = 'New brilliant description';
    topologyTemplates.goToTopologyTemplateDetails(topologyTemplateName);
    xedit.sendKeys('template_' + topologyTemplateName + '_description', newDescription, false);
    expect(element(by.binding('topologyTemplate.description')).getText()).toEqual(newDescription);
  });

  it('Architect should be able to see the topology editor and the version menu element on the detail page', function() {
    topologyTemplates.goToTopologyTemplateDetails(topologyTemplateName);
    topologyTemplates.checkTopologyTemplate(topologyTemplateName);
    expect(element(by.id('env-version-select')).isPresent()).toBe(true);
  });

  it('Component manager should be redirected to error page in case he goes to a topology template url in browser', function() {
    topologyTemplates.goToTopologyTemplateDetails(topologyTemplateName);
    browser.getCurrentUrl().then(function(topologyUrl) {
      authentication.logout();
      authentication.login('componentManager');
      browser.get(topologyUrl);
      browser.waitForAngular();
      browser.getCurrentUrl().then(function(shouldBeRedirectedURL) {
        expect(shouldBeRedirectedURL).toContain('#/restricted');
      });
    });
  });

  it('afterAll', function() { authentication.logout(); });
});
