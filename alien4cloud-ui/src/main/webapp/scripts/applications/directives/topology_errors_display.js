define(function(require) {
  'use strict';

  var modules = require('modules');
  
  var appModule = modules.get('a4c-applications')

  appModule.directive('propertiesErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/properties_errors_display.html',
      restrict: 'E',
      scope: {
        'tasks': '='
      },
      link: function (scope){
      }
    };
  });
  
  appModule.directive('scalabilityErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/scalability_errors_display.html',
      restrict: 'E',
      scope: {
        'tasks': '='
      },
      link: function (scope){
      }
    };
  });
  appModule.directive('nodeFiltersErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/node_filters_errors_display.html',
      restrict: 'E',
      scope: {
        'tasks': '='
      },
      link: function (scope){
      }
    };
  });
  appModule.directive('lowerboundErrorsDisplay', function() {
    return {
      templateUrl: 'views/applications/directives/lowerbound_errors_display.html',
      restrict: 'E',
      scope: {
        'tasks': '='
      },
      link: function (scope){
      }
    };
  });
});
