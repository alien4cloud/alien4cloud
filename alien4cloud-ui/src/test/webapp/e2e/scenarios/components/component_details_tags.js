/* global describe, it, element, by, expect */

'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var xedit = require('../../common/xedit');
var components = require('../../components/components');

var computeDetail = function(){
  components.go();
  components.search('tosca.nodes.Compute');
  common.click(by.id('li_tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT'));
}

var checkTag = function(tagName, expectedValue){
  var tagLine = common.element(by.id('tag_'+tagName));
  expect(common.element(by.binding('tag.name'), tagLine).getText()).toEqual(tagName);
  expect(common.element(by.binding('tag.value'), tagLine).getText()).toEqual(expectedValue);
}

var checkTagNotPresent = function(tagName) {
  expect(element(by.id('tag_'+tagName))).toBeTruthy();
}

describe('Component details tags edition', function() {
  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('componentManager');
    computeDetail();
  });
  
  it('should be able to add a valid tag as a manager', function() {
    var goodTag = components.tags.goodTag;
    var tagForm = common.element(by.name('formAddTag')); 
    var tags = element.all(by.repeater('tag in component.tags'));
    
    common.sendKeys(by.id('input-key'), goodTag.key, tagForm);
    common.sendKeys(by.id('input-value'), goodTag.value, tagForm);
    common.click(by.id('btn-add-tag'));
    expect(tags.count()).toBe(1);
    checkTag(goodTag.key, goodTag.value);
    
    /*check after a refresh of the page*/
    computeDetail();
    expect(tags.count()).toBe(1);
    checkTag(goodTag.key, goodTag.value);
    
  });

  it('should be able to update a tag as a manager', function() {
    var goodTag = components.tags.goodTag;
    var updatedValue = 'this is an updated value';
    
    /* update the tag value */
    xedit.sendKeys('tag_'+goodTag.key, updatedValue);
    
    //This should not add a new tag.
    var tags = element.all(by.repeater('tag in component.tags'));
    expect(tags.count()).toBe(1);
    checkTag(goodTag.key, updatedValue);
    
    // after a refresh
    computeDetail();
    expect(tags.count()).toBe(1);
    checkTag(goodTag.key, updatedValue);
    
    xedit.sendKeys('tag_'+goodTag.key, goodTag.value);
    
  });

  it('should be able to add a second tag as a manager', function() {
    var goodTag2 = components.tags.goodTag2;
    
    //make sure we have only one tag
    var tags = element.all(by.repeater('tag in component.tags'));
    expect(tags.count()).toBe(1);

    var tagForm = common.element(by.name('formAddTag')); 
    common.sendKeys(by.id('input-key'), goodTag2.key, tagForm);
    common.sendKeys(by.id('input-value'), goodTag2.value, tagForm);
    common.click(by.id('btn-add-tag'));
    expect(tags.count()).toBe(2);
    checkTag(goodTag2.key, goodTag2.value);
    
    /*check after a refresh of the page*/
    computeDetail();
    expect(tags.count()).toBe(2);
    checkTag(goodTag2.key, goodTag2.value);
  });

  it('should keep button (+) disabled when typing a bad tag', function() {
    var badTag = components.tags.badTag;
    computeDetail();
    var tagForm = common.element(by.name('formAddTag')); 
    common.sendKeys(by.id('input-key'), badTag.key, tagForm);
    common.sendKeys(by.id('input-value'), badTag.value, tagForm);
    expect(element(by.id('btn-add-tag')).isEnabled()).toBeFalsy();
    
    checkTagNotPresent(badTag.key)
  });
  
  it('should be able to delete a tag as a manager', function() {
    var goodTag2 = components.tags.goodTag2;
    computeDetail();
    var tags = element.all(by.repeater('tag in component.tags'));
    expect(tags.count()).toBe(2);
    checkTag(goodTag2.key, goodTag2.value);
    
    common.click(by.id('delete_tag_'+goodTag2.key));
    expect(tags.count()).toBe(1);
    checkTagNotPresent(goodTag2.key)
    
    computeDetail();
    expect(tags.count()).toBe(1);
    checkTagNotPresent(goodTag2.key)
    
  });

  it('Component browsr should not be able to add, edit or delete tags',function(){
    authentication.reLogin('componentBrowser');
    computeDetail();
    
    var goodTag = components.tags.goodTag;
    checkTag(goodTag.key, goodTag.value);
    //no add form
    expect(element(by.name('formAddTag')).isPresent()).toBe(false);
    
    //not editable
    var tagLine = common.element(by.id('tag_'+goodTag.key));
    expect(tagLine.element(by.css('.editable-click')).isPresent()).toBe(false);
    
    //no delete button
    expect(element(by.id('delete_tag_'+goodTag.key)).isPresent()).toBe(false);
    
  })
  it('afterAll', function() { authentication.logout(); });
});
