/* global element, by */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var csars= require('../../components/csars');
var git = csars.git;

var testRepo3Id = '1ef39733-d8c4-4f99-ba01-9b77e5693055';

var csarGitRepositoriesData = require(__dirname + '/_data/csar_git/csargitrepositories.json');

describe('CSARS management via git: list and search', function() {

  it('beforeAll', function() {
    setup.setup();
    setup.index("csargitrepository", "csargitrepository", csarGitRepositoriesData);
    common.home();
    authentication.login('componentManager');
    git.go();
  });
  
  it('component browser should not be able to browse csar git repositories', function(){
    authentication.reLogin('componentBrowser');
    common.click(by.id('menu.components'));
    common.isNotNavigable('components', 'git');
  });
  
  it('admin and component manager should be able to browse csar git repositories ', function(){
    authentication.reLogin('admin');
    common.click(by.id('menu.components'));
    common.isNavigable('components', 'git');
    
    authentication.reLogin('componentManager');
    common.click(by.id('menu.components'));
    common.isNavigable('components', 'git');
  });
  
  it('Component manager should be able to list git repositories and check pagination', function() {
    git.go();
    expect(element(by.id('search-container')).isPresent()).toBe(true);
    expect(element(by.id('result-container')).isPresent()).toBe(true);

    var results = element.all(by.repeater('gitRepository in searchResult.data'));
    expect(results.count()).toEqual(20);

    // pagination
    var pagination = element.all(by.repeater('page in pages'));
    expect(pagination.count()).toEqual(6); // First, Previous, 1, 2, Next, Last
    // go to the second page and check
    var secondPageElement = pagination.get(3);
    common.click(by.tagName('a'), secondPageElement);
    results = element.all(by.repeater('gitRepository in searchResult.data'));
    expect(results.count()).toEqual(1);
  });
  
  it('should be able to use search to find csr git repositories', function() {
    git.go();
    git.search('repo');
    var results = element.all(by.repeater('gitRepository in searchResult.data'));
    expect(results.count()).toBeGreaterThan(0);
    
    git.search('repo-3');
    results = element.all(by.repeater('gitRepository in searchResult.data'));
    expect(results.count()).toEqual(1);
    expect(common.element(by.id('gitRepository_'+testRepo3Id))).toBeTruthy();
    
    //case nothing 
    git.search('dragonBallZ');
    results = element.all(by.repeater('gitRepository in searchResult.data'));
    expect(common.element(by.tagName('empty-place-holder'))).toBeTruthy();
    expect(results.count()).toEqual(0);
    
  });

  it('component manager should be able to delete a gitRepository', function() {
    git.go();
    // try to delete the tosca-base-types csars and check errors
    common.deleteWithConfirm('delete-'+testRepo3Id, true);
    
    //check no errors
    toaster.expectNoErrors();
    
    //check the csar still exist
    git.search('repo-3');
    expect(element(by.id('gitRepository_'+testRepo3Id)).isPresent()).toBe(false);
    //after a refresh
    git.go();
    git.search('repo-3');
    expect(element(by.id('gitRepository_'+testRepo3Id)).isPresent()).toBe(false);
  });
  
  it('afterAll', function() { authentication.logout(); });
  
});
