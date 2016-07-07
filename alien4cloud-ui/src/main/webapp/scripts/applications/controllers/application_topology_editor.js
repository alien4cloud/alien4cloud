define(function (require) {
  'use strict';

  var modules = require('modules');

  require('angular-ui-select');
  modules.get('a4c-applications', ['ui.select']);

  var registerService = require('scripts/topology/editor_register_service');
  registerService('applications.detail.topology');
}); // define
