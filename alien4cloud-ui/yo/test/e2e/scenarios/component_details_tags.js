/* global element, by */
'use strict';

var authentication = require('../authentication/authentication');
var common = require('../common/common');
var components = require('../components/components');

var computeComponent = {
  id: 'tosca.nodes.Compute:2.0',
  elementId: 'tosca.nodes.Compute',
  archiveVersion: '2.0'
};

// Tags definition
var goodTag = {
  'key': 'my_good_tag',
  'value': 'Whatever i want to add as value here...'
};

var goodTag2 = {
  'key': 'my_good_tag2',
  'value': 'Whatever i want to add as value here for ...'
};

var badTag = {
  'key': 'my_good*tag',
  'value': 'This tag should not be added to tag list with *...'
};

describe('Component details tags edition', function() {
  /* Before each spec in the tests suite */
  beforeEach(function() {
    common.before();
    /* Login */
    authentication.login('componentManager');
  });

  /* After each spec in the tests suite(s) */
  afterEach(function() {
    // Logout action
    common.after();
  });

  it('should go on #/components and select details for the first element', function() {
    console.log('################# should go on #/components and select details for the first element');
    // ensure i've data on my ES
    components.goToComponentDetailPage(computeComponent.id);
  });

  var tags = element.all(by.repeater('tag in component.tags'));
  it('should add a good tag without errors', function() {
    console.log('################# should add a good tag without errors');
    components.goToComponentDetailPage(computeComponent.id);

    tags.count().then(function() {

      /* Add a new valid tag i should have the tags count +1 */
      element(by.model('newTag.key')).sendKeys(goodTag.key);
      element(by.model('newTag.val')).sendKeys(goodTag.key);

      /* Click to add the new element */
      var btnAddTag = browser.element(by.id('btn-add-tag'));
      btnAddTag.click();

      /* New tags count is the same after the bad tag add */
      expect(tags.count()).toBeGreaterThan(0);

      /* Add the same tag a second time : case update tag */
      element(by.model('newTag.key')).sendKeys(goodTag.key);
      element(by.model('newTag.val')).sendKeys(goodTag.key);

      /* Click to add the good tag twice */
      btnAddTag.click();

      /* New tags count is the same after the bad tag add */
      expect(tags.count()).toBeGreaterThan(0);

      /* Add the same tag a second time : case update tag */
      element(by.model('newTag.key')).sendKeys(goodTag2.key);
      element(by.model('newTag.val')).sendKeys(goodTag2.key);
      btnAddTag.click();

      /* New tags count at least at 1 */
      expect(tags.count()).toBeGreaterThan(1);

    });

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
});
