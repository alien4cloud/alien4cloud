/* global describe, it, expect, browser */

'use strict';

var setup = require('../../common/setup');
var common = require('../../common/common');
var screenshot = require('../../common/screenshot');

describe('Homepage', function() {
  it('beforeAll', function() { setup.setup(); });

  it('should have `ALIEN 4 Cloud` as page title', function() {
    screenshot.take('main-homepage');
    common.home();
    expect(browser.getTitle()).toEqual('ALIEN 4 Cloud');
  });
});
