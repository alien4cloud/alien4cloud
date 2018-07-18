define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor').directive('propertiesErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/properties_errors_display.html',
      restrict: 'E',
      scope: {
        'stepTasks': '=tasks'
      },
      link: function (){
      }
    };
  });
  modules.get('a4c-topology-editor').directive('scalabilityErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/scalability_errors_display.html',
      restrict: 'E',
      scope: {
        'stepTasks': '=tasks'
      },
      link: function (){
      }
    };
  });
  modules.get('a4c-topology-editor').directive('nodeFiltersErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/node_filters_errors_display.html',
      restrict: 'E',
      scope: {
        'stepTasks': '=tasks',
        'inputColumn': '='
      },
      link: function (scope){
        scope._ = _;
      }
    };
  });
  modules.get('a4c-topology-editor').directive('lowerboundErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/lowerbound_errors_display.html',
      restrict: 'E',
      scope: {
        'stepTasks': '=tasks'
      },
      link: function (){
      }
    };
  });
  modules.get('a4c-topology-editor').directive('inputArtifactsErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/input_artifacts_errors_display.html',
      restrict: 'E',
      scope: {
        'stepTasks': '=tasks'
      },
      link: function (){
      }
    };
  });
  modules.get('a4c-topology-editor').directive('artifactsErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/artifacts_errors_display.html',
      restrict: 'E',
      scope: {
        'stepTasks': '=tasks'
      },
      link: function (){
      }
    };
  });
});
