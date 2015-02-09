/* global by, element */

'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var topologyEditorCommon = require('../../topology/topology_editor_common');
var topologyTemplates = require('../../topology/topology_templates_common');
var applications = require('../../applications/applications');

describe('Applications management :', function() {
  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    authentication.login('applicationManager');
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should be able to go to application search page at ' + common.appSearchUrl + ' and create new application', function() {
    console.log('################# should be able to go to application search page at ' + common.appSearchUrl + ' and create new application');
    applications.createApplication('Alien', 'Great Application');
    // Go to the app details page
    expect(browser.element(by.binding('application.name')).getText()).toEqual('Alien');
    expect(browser.element(by.binding('application.description')).getText()).toEqual('Great Application');
  });

  it('should create first a topology template with 3 node templates (COMPUTE, JAVA, TOMCATRPM) and an application based on this template', function() {
    console.log('################# should create first a topology template with 3 node templates (COMPUTE, JAVA, TOMCATRPM) and an application based on this template');
    // Login as architect to create a topology template
    authentication.reLogin('architect');

    // create a topology template with one Compute, 1 Java and 1 Tomcat RPM
    topologyTemplates.createTopologyTemplateWithNodesAndRelationships(topologyEditorCommon.topologyTemplates.template4);

    // create a new application with this template as model
    authentication.reLogin('applicationManager');
    // template at index 2 is the first and only one created (index 1 equals is the default, no template)
    applications.createApplication('JavaTomcatWarApplication', 'Simple application with one compute, one template and all that around java...', 2);

    // check the created topology application (count nodes, count relationship..)
    applications.goToApplicationDetailPage('JavaTomcatWarApplication', true);

    // check relationship count
    topologyEditorCommon.checkNumberOfRelationshipForANode('rect_JavaRPM', 2);
    topologyEditorCommon.checkNumberOfRelationshipForANode('rect_Compute_2', 0);
    topologyEditorCommon.checkNumberOfRelationshipForANode('rect_Compute', 0);
  });

  it('should be able to delete an application from the search list and the application\'s detail page', function() {
    console.log('################# should be able to delete an application from the search list and the application\'s detail page');
    applications.createApplication('Alien_1', 'Great Application 1');
    applications.createApplication('Alien_2', 'Great Application 2');
    applications.createApplication('Alien_3', 'Great Application 3');
    applications.goToApplicationListPage('main', 'applications');
    // delete application from the search list
    common.deleteWithConfirm('delete-app_Alien_1', true);
    expect(element(by.id('app_Alien_1')).isPresent()).toBe(false);

    // cancel a delete action
    common.deleteWithConfirm('delete-app_Alien_3', false);
    expect(element(by.id('app_Alien_3')).isPresent()).toBe(true);

    // delete application from it detail page
    applications.goToApplicationDetailPage('Alien_2');
    common.deleteWithConfirm('btn-delete-app', true);
    expect(element(by.id('app_Alien_2')).isPresent()).toBe(false);
  });

  it('should be able to edit an application', function() {
    console.log('################# should be able to rename an application');
    // Create an application
    applications.createApplication('MyNewApplication1', 'A brand new application...');
    applications.createApplication('MyNewApplication2', 'A rband new application...');
    applications.goToApplicationDetailPage('MyNewApplication1');

    // Rename the application with a non existing name
    common.sendValueToXEditable('app-name', 'NewNameForMyApp', false);
    expect(element(by.binding('application.name')).getText()).toEqual('NewNameForMyApp');
    common.expectNoErrors();

    // Change the application's description
    common.sendValueToXEditable('app-desc', 'Some Description', false, 'textarea');
    expect(element(by.binding('application.description')).getText()).toEqual('Some Description');
    common.expectNoErrors();

    // Rename with an existing application name
    common.sendValueToXEditable('app-name', 'MyNewApplication2', false);
    common.abortXEditable('app-name');
    common.expectTitleMessage('409'); // server 'AlreadyExist' exception error code
    common.dismissAlert();
  });

});
