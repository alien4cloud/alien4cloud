/* global describe, it, element, by, expect */

'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var components = require('../../components/components');

describe('Component details tags edition', function() {
  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to add a valid tag', function() {
    var goodTag = components.tags.goodTag;
    components.goToComponentDetailPage(computeComponent.id);
    var tags = element.all(by.repeater('tag in component.tags'));
    tags.count().then(function() {
      var btnAddTag = element(by.id('btn-add-tag'));

      /* Add a new valid tag i should have the tags count +1 */
      element(by.model('newTag.key')).sendKeys(goodTag.key);
      element(by.model('newTag.val')).sendKeys(goodTag.key);
      btnAddTag.click();

      /* New tags count is the same after the bad tag add */
      expect(tags.count()).toBe(1);
    });
  });

  it('should be able to update a tag', function() {
    var goodTag = components.tags.goodTag;
    var btnAddTag = element(by.id('btn-add-tag'));

    /* update a tag */
    element(by.model('newTag.key')).sendKeys(goodTag.key);
    element(by.model('newTag.val')).sendKeys(goodTag.key);
    btnAddTag.click();

    /* This should not add a new tag. */
    var tags = element.all(by.repeater('tag in component.tags'));
    expect(tags.count()).toBe(1);
  });

  it('should be able to add a second tag', function() {
    var goodTag2 = components.tags.goodTag2;
    var btnAddTag = element(by.id('btn-add-tag'));

    /* add another tag */
    element(by.model('newTag.key')).sendKeys(goodTag2.key);
    element(by.model('newTag.val')).sendKeys(goodTag2.key);
    btnAddTag.click();

    /* New tags count at least at 1 */
    var tags = element.all(by.repeater('tag in component.tags'));
    expect(tags.count()).toBe(2);
  });

  it('should keep button (+) disabled when typing a bad tag', function() {
    console.log('################# should keep button (+) disabled when typing a bad tag');
    components.goToComponentDetailPage(computeComponent.id);
    tags.count().then(function() {

      /* Add a bad tag i should have the tags count +1 */
      element(by.model('newTag.key')).sendKeys(badTag.key);
      element(by.model('newTag.val')).sendKeys(badTag.key);

      /* Click to add the new element */
      var btnAddTag = browser.element(by.id('btn-add-tag'));
      expect(btnAddTag.isEnabled()).toBe(false);

    });

  });

  it('afterAll', function() { authentication.logout(); });
});
