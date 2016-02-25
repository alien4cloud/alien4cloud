/* global element, by, expect, describe, it */
'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var toaster = require('../../common/toaster');
var authentication = require('../../authentication/authentication');
var csars = require('../../components/csars');
var git = csars.git;

var testRepo = {
  url: 'http://github.com/created',
  branchId: 'fakeBranch',
  subPath: 'fakeSubPath',
  subPath2: 'fakeSubPath2',
  newUrl: 'http://github.com/newCreated'
};

var testRepo2 = {
  url: 'http://github.com/created2',
  branchId: 'fakeBranch',
  subPath: 'fakeSubPath',
};

var checkSaveButton = function(enabled) {
  expect(common.element(by.id('btn-save')).isEnabled()).toBe(enabled);
};

var checkAddLocationButton = function(enabled) {
  expect(common.element(by.id('btn-add-location')).isEnabled()).toBe(enabled);
};

var checkGitRepoInstances = function(url, numberOfInstance) {
  git.search(url);
  var results = element.all(by.repeater('gitRepository in searchResult.data'));
  expect(results.count()).toEqual(numberOfInstance);
  if (numberOfInstance === 0) {
    expect(common.element(by.tagName('empty-place-holder'))).toBeTruthy();
  }
};

describe('CSARS management via git', function() {

  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('componentManager');
    git.go();
  });

  it('should be able to add a new csar git repository ', function() {
    element(by.id('btn-new-gitRepository')).click();

    //first check the save button is disabled
    checkSaveButton(false);

    //fill in url
    common.sendKeys(by.id('url'), testRepo.url);
    checkAddLocationButton(false);
    checkSaveButton(false);

    //add a location: branch only
    common.sendKeys(by.id('branchId'), testRepo.branchId);
    checkAddLocationButton(true);
    checkSaveButton(false);
    //after adding at least one location, the save button should be enabled
    common.click(by.id('btn-add-location'));
    checkSaveButton(true);

    //try to add the same location: branch only, the add-location-button should not be enabled
    common.sendKeys(by.id('branchId'), testRepo.branchId);
    checkAddLocationButton(false);
    checkSaveButton(true);

    //add a location: branch + subPath
    common.sendKeys(by.id('subPath'), testRepo.subPath);
    checkAddLocationButton(true);
    checkSaveButton(true);
    common.click(by.id('btn-add-location'));

    //check we cannot add the previuos again
    common.sendKeys(by.id('branchId'), testRepo.branchId);
    common.sendKeys(by.id('subPath'), testRepo.subPath);
    checkAddLocationButton(false);
    common.clear(by.id('branchId'));
    common.clear(by.id('subPath'));


    //save the form
    common.click(by.id('btn-save'));

    //check it was really created
    checkGitRepoInstances(testRepo.url, 1);

    //check what is displayed
    var repoLine = element.all(by.repeater('gitRepository in searchResult.data')).first();
    var repoRows = repoLine.all(by.tagName('td'));

    //url
    expect(repoRows.first().getText()).toEqual(testRepo.url);
    //archive's folders
    expect(repoRows.get(1).getText()).toContain('*');
    expect(repoRows.get(1).getText()).toContain(testRepo.subPath);
    //branch
    expect(repoRows.get(2).getText()).toEqual(testRepo.branchId + '\n' + testRepo.branchId);

    git.go();
    checkGitRepoInstances(testRepo.url, 1);

  });

  it('Should be able to cancel the creation of csar git repository', function() {
    git.go();
    common.click(by.id('btn-new-gitRepository'));
    common.sendKeys(by.id('url'), testRepo2.url);
    common.sendKeys(by.id('branchId'), testRepo2.branchId);
    common.click(by.id('btn-add-location'));
    common.click(by.id('btn-cancel'));

    checkGitRepoInstances(testRepo2.url, 0);
    git.go();
    checkGitRepoInstances(testRepo2.url, 0);
  });

  it('Should not be able to add a csar git repository with an url already existing', function() {
    git.go();
    common.click(by.id('btn-new-gitRepository'));
    common.sendKeys(by.id('url'), testRepo.url);
    common.sendKeys(by.id('branchId'), 'Sauron');
    common.click(by.id('btn-add-location'));
    common.click(by.id('btn-save'));

    //check errors
    toaster.expectErrors();
    toaster.expectMessageToContain('Object already exists');
    toaster.dismissIfPresent();

    //check the repo not added
    checkGitRepoInstances(testRepo.url, 1);
    git.go();
    checkGitRepoInstances(testRepo.url, 1);
  });

  it('Should be able to edit a csar git repository', function() {
    git.go();
    checkGitRepoInstances(testRepo.url, 1);
    var repoLine = element.all(by.repeater('gitRepository in searchResult.data')).first();
    common.click(by.css('button[id^="edit_"]'), repoLine);

    expect(common.element(by.id('url')).getAttribute('value')).toEqual(testRepo.url);

    //change url and add a new location
    common.sendKeys(by.id('url'), testRepo.newUrl);
    common.sendKeys(by.id('branchId'), testRepo.branchId);
    common.sendKeys(by.id('subPath'), testRepo.subPath2);
    common.click(by.id('btn-add-location'));
    common.click(by.id('btn-save'));

    toaster.expectNoErrors();
    checkGitRepoInstances(testRepo.url, 0);
    checkGitRepoInstances(testRepo.newUrl, 1);

    repoLine = element.all(by.repeater('gitRepository in searchResult.data')).first();
    var repoRows = repoLine.all(by.tagName('td'));
    //check
    //archive's folders
    expect(repoRows.get(1).getText()).toContain('*');
    expect(repoRows.get(1).getText()).toContain(testRepo.subPath);
    expect(repoRows.get(1).getText()).toContain(testRepo.subPath2);
    //branch
    expect(repoRows.get(2).getText()).toEqual(testRepo.branchId + '\n' + testRepo.branchId + '\n' + testRepo.branchId);

  });

  it('afterAll', function() {
    authentication.logout();
  });

});
