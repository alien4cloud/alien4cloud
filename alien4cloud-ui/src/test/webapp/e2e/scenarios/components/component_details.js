/* global describe, it, by, element */
'use strict';

var setup = require('../../common/setup');
var authentication = require('../../authentication/authentication');
var common = require('../../common/common');
var components = require('../../components/components');

describe('Component details', function() {
  /* Before each spec in the tests suite */
  it('beforeAll', function() {
    setup.setup();
    common.home();
    authentication.login('admin');
  });

  it('should be able to see a component details.', function() {
  });

  it('should be able to see the content of an archive from a component detail.', function(){
  });

  it('afterAll', function() { authentication.logout(); });
});
