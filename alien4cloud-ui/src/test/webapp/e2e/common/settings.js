/**
* Contains some global variables for the e2e ui tests for Alien 4 Cloud
*/

'use strict';

// Enable debugs logs.
var debug = false;
// Get the version from the build (used to find the correct versions of the mock-plugins)
var version = require(__dirname + '/../../../../../target/alien4cloud-ui-war/version.json');

module.exports.debug = debug;
module.exports.version = version;
