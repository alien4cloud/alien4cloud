define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  states.state('catalog.components.detail.info', {
    url: '/info',
    templateUrl: 'views/components/components_info.html',
    controller: 'ComponentInfoCtrl',
  });

  modules.get('a4c-components', ['ngResource', 'ui.bootstrap', 'ui.router', 'a4c-auth']).controller('ComponentInfoCtrl',
    ['$scope',
    function($scope) {

    }
  ]); // controller
});// define
