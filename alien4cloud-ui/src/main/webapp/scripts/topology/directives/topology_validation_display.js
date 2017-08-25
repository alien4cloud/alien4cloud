// Directive allowing to display a topology
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/topology/directives/topology_errors_display');

  modules.get('a4c-topology-editor').directive('topologyValidationDisplay',
    [
    function() {
      return {
        restrict: 'E',
        templateUrl: 'views/topology/topology_validation_display.html',
        controller: 'ValidationDisplayCtrl',
        scope: {
          validationDto: '=',
        }
      };
    }
  ]); // directive

  modules.get('a4c-topology-editor', []).controller('ValidationDisplayCtrl', ['$scope', function ($scope) {
    $scope._ = _;
  }]);
}); // define
