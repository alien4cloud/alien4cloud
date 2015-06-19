define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-auth').directive('alienNavBar',
    function() {
      return {
        templateUrl: 'views/authentication/navbar.html',
        restrict: 'E'
      };
    });
});
