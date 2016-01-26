/* global element, by */
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
};
var git = {
    name:'git-type',
    version: '2.0.0-SNAPSHOT',
    id: function(){
      return this.name +':'+ this.version;
    }
};

describe('CSAR list', function() {

  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('componentManager');
  });
  
  it('component browser should not be able to browse csars', function(){
    authentication.reLogin('componentBrowser');
    common.click(by.id('menu.components'));
    common.isNotNavigable('components', 'csars');
  });
  
  it('admin and component manager should be able to browse csars ', function(){
    authentication.reLogin('admin');
    common.click(by.id('menu.components'));
    common.isNavigable('components', 'csars');
    
    authentication.reLogin('componentManager');
    common.click(by.id('menu.components'));
    common.isNavigable('components', 'csars');
  });
  
  it('Component browser should be able to list arhives and check pagination', function() {
    csars.go();
    expect(element(by.id('search-container')).isPresent()).toBe(true);
    expect(element(by.id('result-container')).isPresent()).toBe(true);

    var results = element.all(by.repeater('csar in searchResult.data'));
    expect(results.count()).toEqual(20);

    // pagination
    var pagination = element.all(by.repeater('page in pages'));
    expect(pagination.count()).toEqual(6); // First, Previous, 1, 2, Next, Last
    // go to the second page and check
    var secondPageElement = pagination.get(3);
    common.click(by.tagName('a'), secondPageElement);
    results = element.all(by.repeater('csar in searchResult.data'));
    expect(results.count()).toEqual(1);
  });

  it('should be able to use search to find archives', function() {
    csars.go();
    csars.search(tomcatWar.name);
    var results = element.all(by.repeater('csar in searchResult.data'));
    expect(results.count()).toBeGreaterThan(0);
    expect(common.element(by.id('csar_'+tomcatWar.id()))).toBeTruthy();
    
    csars.search(git.name);
    results = element.all(by.repeater('csar in searchResult.data'));
    expect(results.count()).toBeGreaterThan(0);
    expect(common.element(by.id('csar_'+git.id()))).toBeTruthy();
    
    //case nothing 
    csars.search('dragonBallZ');
    results = element.all(by.repeater('csar in searchResult.data'));
    expect(common.element(by.tagName('empty-place-holder'))).toBeTruthy();
    expect(results.count()).toEqual(0);
    
  });
  
  it('Component manager or admin should be able to  see the delete button on an archive', function(){
    csars.go();
    csars.search(tomcatWar.name);
    expect(element(by.id('delete-csar_'+tomcatWar.id())).isPresent()).toBe(true);
    
    authentication.reLogin('admin');
    csars.go();
    csars.search(tomcatWar.name);
    expect(element(by.id('delete-csar_'+tomcatWar.id())).isPresent()).toBe(true);
  });
  
  
  it('afterAll', function() { authentication.logout(); });

});
