/* global element, by */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var csars = require('../../components/csars');

var applicationsData = require(__dirname + '/_data/csar/applications.json');
var applicationEnvironmentsData = require(__dirname + '/_data/csar/applicationenvironments.json');
var applicationVersionsData = require(__dirname + '/_data/csar/applicationversions.json');
var groupsData = require(__dirname + '/_data/csar/groups.json');
var topologiesData = require(__dirname + '/_data/csar/topologies.json');

var tomcatWar = {
    name:'tomcat-war-types',
    version: '2.0.0-SNAPSHOT',
    id: function(){
      return this.name +':'+ this.version;
    }
}
var git = {
    name:'git-type',
    version: '2.0.0-SNAPSHOT',
    id: function(){
      return this.name +':'+ this.version;
    }
}

var checkCsarExists = function(name, version, exists){
  if( typeof exists === 'undefined' || exists ===null){
    exists = true;
  }
  csars.search(name);
  expect(element(by.id('csar_'+name+':'+version)).isPresent()).toBe(exists);
}

describe('CSAR deletion', function() {

  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    setup.index("application", "application", applicationsData);
    setup.index("applicationEnvironment", "applicationEnvironment", applicationEnvironmentsData);
    setup.index("applicationversion", "applicationversion", applicationVersionsData);
    setup.index("group", "groups", groupsData);
    setup.index("topology", "topology", topologiesData);
    common.home();
    authentication.login('admin');
    csars.go();
  });

  it('should not be able to delete from casr list, a CSAR referenced by an application / csars /  topologytemplate', function() {
    
    checkCsarExists(tomcatWar.name, tomcatWar.version);
    
    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-csar_'+tomcatWar.id(), true);
    
    //check errors
    toaster.expectErrors();
    toaster.expectMessageToContain('apache-load-balancer (csar)');
    toaster.expectMessageToContain('tomcat (application)');
    toaster.expectMessageToContain('tomcat-war (topologytemplate)');
    
    //check the csar still exist
    checkCsarExists(tomcatWar.name, tomcatWar.version);
    csars.go();
    checkCsarExists(tomcatWar.name, tomcatWar.version);
    
  });

  it('should not be able to delete fom csar detail page, a CSAR referenced by an application / csars /  topologytemplate', function() {
    
    csars.open(tomcatWar.name, tomcatWar.version);
      
    // check the "linked resources list"
    var results = element.all(by.repeater('resource in csar.relatedResources'));
    expect(results.count()).toBeGreaterThan(0);
    
    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-csar_'+tomcatWar.id(), true);
    
    //check errors
    toaster.expectErrors();
    toaster.expectMessageToContain('apache-load-balancer (csar)');
    toaster.expectMessageToContain('tomcat (application)');
    toaster.expectMessageToContain('tomcat-war (topologytemplate)');
    
    //check we are stil on the detail page
    expect(common.element(by.id('csar-name')).getText()).toEqual(tomcatWar.name);
    expect(common.element(by.id('csar-version')).getText()).toEqual(tomcatWar.version);
    results = element.all(by.repeater('resource in csar.relatedResources'));
    expect(results.count()).toBeGreaterThan(0);
    
    //check the csar still exist
    csars.go();
    checkCsarExists(tomcatWar.name, tomcatWar.version);
  });

  it('should be able to delete a non referenced CSAR', function() {
    csars.open(git.name, git.version);
    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-csar_'+git.id(), true);
    
    //check no errors
    toaster.expectNoErrors();
    
    //check the csar still exist
    checkCsarExists(git.name, git.version, false);
    csars.go();
    checkCsarExists(git.name, git.version, false);

  });
  
  it('afterAll', function() { authentication.logout(); });

});
