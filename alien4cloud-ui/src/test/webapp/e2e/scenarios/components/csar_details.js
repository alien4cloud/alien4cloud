/* global describe, it, by, element */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var authentication = require('../../authentication/authentication');
var csars = require('../../components/csars');

var tomcatWar = {
    name:'tomcat-war-types',
    version: '2.0.0-SNAPSHOT',
    id: function(){
      return this.name +':'+ this.version;
    }
}

describe('Csar details', function() {
  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('componentManager');
  });

  it('should be able to see an archive details.', function() {
    csars.go();
    csars.open(tomcatWar.name, tomcatWar.version);

    //check we are indeed on the detail page
    expect(common.element(by.id('csar-name')).getText()).toEqual(tomcatWar.name);
    expect(common.element(by.id('csar-version')).getText()).toEqual(tomcatWar.version);
    var results = element.all(by.repeater('resource in csar.relatedResources'));
    expect(results.count()).toBeGreaterThan(0);
  });

  it('should be able to see the content of an archive from a component detail.', function(){
    csars.go();
    csars.open(tomcatWar.name, tomcatWar.version);
    var tomcatWarResource = {
        name: 'tomcat-war',
        version: '0.1.0-SNAPSHOT',
        id: function(){
          return this.name+':'+this.version;
        }
    }

    var resourceLine = common.element(by.id('res_'+tomcatWarResource.id()));
    var rows = resourceLine.all(by.tagName('td'));
    //the last row is a link to the resource detail
    //TODO: find a way to test for application and topologyTemplate cases
    common.click(by.tagName('a'), rows.last());

    //check we are indeed on the detail page
    expect(common.element(by.id('csar-name')).getText()).toEqual(tomcatWarResource.name);
    expect(common.element(by.id('csar-version')).getText()).toEqual(tomcatWarResource.version);
  });


  it('afterAll', function() { authentication.logout(); });
});
