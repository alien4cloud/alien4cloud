/* global describe, it, browser */
'use strict';

describe('Initialize test environment', function() {
  it('Browser init', function() {
    browser.driver.manage().window().setSize(1920, 1080);
    browser.driver.manage().window().maximize();
    browser.driver.manage().window().getSize().then(function(size) {
      console.log('################# Window\'s size  [' + size.width + ', ' + size.height + ']');
    });
  });
});
