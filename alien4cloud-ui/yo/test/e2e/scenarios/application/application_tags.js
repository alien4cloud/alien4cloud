/* global by, element */
'use strict';

var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var applications = require('../../applications/applications');

// Tags definition
var goodTag = {
  'key': 'my_good_tag',
  'value': 'Whatever i want to add as value here...'
};

var goodTag2 = {
  'key': 'my_good_tag2',
  'value': 'Whatever i want to add as value here for ...'
};

describe('Application Tags :', function() {

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

  it('should add a good tag without errors', function() {
    console.log('################# should add a good tag without errors');
    applications.createApplication('Alien', 'Great Application');

    // This component is supposed to be new and has no tags
    var tags = element.all(by.repeater('tag in application.tags'));
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
      expect(tags.count()).toEqual(2);

    });
    /* Then end */
  });
});
