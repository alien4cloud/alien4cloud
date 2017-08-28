define(function (require) {
  'use strict';

  var modules = require('modules');
  var $ = require('jquery');
  var _ = require('lodash');

  const filterPattern = /^([\w,\.]+)=\[?([\"\w,\,\s]+)\]?$/;

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
  modules.get('a4c-common', []).controller('SearchCtrl', ['$scope', '$timeout', function ($scope, $timeout) {
    $scope.searchService.filtered(true);
    $scope.searching = false;
    $scope.queryManager.onSearchCompleted = function (searchResponse) {
      $scope.queryManager.searchResult = _.defined(searchResponse.data) ? searchResponse.data : undefined;
      $scope.searching = false;
    };
    $scope.searchPadding = 6;

    $scope.searchBoxContent = undefined;

    function updateSize() {
      $timeout(function() {
        var container = $('#search-filters');
        $scope.searchPadding = container.width();
      });
    }
    updateSize();

    $scope.search = function() {
      // detect filter pattern
      var match;
      if(filterPattern.test($scope.searchBoxContent)) {
        match = filterPattern.exec($scope.searchBoxContent);
        var key = match[1];
        var value = match[2];
        var values = value.split(',');
        if(values.length > 1) {
          value = [];
          _.each(values, function(valueItem) {
            value.push(valueItem.trim());
          });
        } else {
          value = [value.trim()];
        }
        _.set($scope.queryManager, ['filters', key], value);
        $scope.searchBoxContent = '';
      } else {
        $scope.queryManager.query = $scope.searchBoxContent;
      }
      $scope.searching = true;
      $scope.searchService.search();
      updateSize();
    };

    $scope.editFilter = function(key) {
      // remove the filter while in edition (will be added back on enter or escape key press)
      var value = $scope.queryManager.filters[key][0];
      if($scope.queryManager.filters[key].length > 1) {
        for(var i = 1; i < $scope.queryManager.filters[key].length; i++) {
          value += ',' + $scope.queryManager.filters[key][i];
        }
        value = '[' + value + ']';
      }
      $scope.searchBoxContent = key + '=' + value;
      delete $scope.queryManager.filters[key];
      updateSize();
    };

    $scope.removeFilter = function(key) {
      delete $scope.queryManager.filters[key];
      updateSize();
    };

    $scope.searchService.search();
  }]);
});
