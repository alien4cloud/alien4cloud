'use strict';

var common = require('../common/common');
var SCREENSHOT = require('../common/screenshot');

describe('Homepage', function() {
  // Load up a view and wait for it to be done with its rendering and epicycles.
  beforeEach(function() {
    common.before();
  });

  // All tests
  it('should have `ALIEN 4 Cloud` as title', function() {
    SCREENSHOT.takeScreenShot('main-homepage');
    expect(browser.getTitle()).toEqual('ALIEN 4 Cloud');
  });
});
