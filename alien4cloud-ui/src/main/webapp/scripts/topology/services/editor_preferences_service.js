// define the rest api elements to work with topology edition.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-topology-editor').factory('editorPreferencesService',[
    function() {
      return {
        'autoValidation' : false
      };
    }
  ]);
}); // define
