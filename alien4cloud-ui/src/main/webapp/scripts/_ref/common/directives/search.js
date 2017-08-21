define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common', []).directive('a4cSearch', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/_ref/common/search.html',
      controller: 'SearchCtrl',
      scope: {
        // The object that stores query data (query text, filters and results)
        queryManager: '=',
        // The service responsible for the search
        searchService: '=',
        // the prefix for all label, it's useful for translation
        labelPrefix: '@'
      }
    };
  });
  //'searchServiceFactory'
  modules.get('a4c-common', []).controller('SearchCtrl', ['$scope', function ($scope) {
    $scope.queryManager.onSearchCompleted = function (searchResponse) {
      $scope.queryManager.searchResult = _.defined(searchResponse.data) ? searchResponse.data : undefined;
    };
    $scope.search = function() {
      $scope.searchService.search();
    };

    $scope.searchService.search();
  }]);
});
