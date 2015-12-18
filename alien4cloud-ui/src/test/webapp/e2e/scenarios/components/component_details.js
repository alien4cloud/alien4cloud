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
    components.go();
    common.click(by.id('li_tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT'));

  });

  it('should be able to see a component details in a older version', function() {
    components.go();
    // change version
    common.click(by.id('tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT_versions'));
    common.click(by.id('tosca.nodes.Compute:1.0.0.wd06-SNAPSHOT_version_1.0.0.wd03-SNAPSHOT'));
    // click
    common.click(by.id('li_tosca.nodes.Compute:1.0.0.wd03-SNAPSHOT'));
  });


  it('should be able to see the content of an archive from a component detail.', function(){
  });


  it('afterAll', function() { authentication.logout(); });
});
