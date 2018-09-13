define(function (require) {
  'use strict';
  var _ = require('lodash');
  var modules = require('modules');
  require('scripts/common/directives/date_time_form');
  require('scripts/applications/services/application_services');
  require('scripts/applications/services/application_environment_services');

  modules.get('alien4cloud-premium-logs').directive('logSearchPanel',
      function () {
        return {
          templateUrl: 'views/log/log_search_panel.html',
          controller: 'LogSearchPanelController',
          restrict: 'E',
          scope: {
            deploymentId: '=',
            instanceId: '=',
            executionId: '=',
            taskId: '=',
            onSearch: '&',
            enableAutoRefrest: '=',
            onAutoRefreshToggled: '&'
          }
        };
      });

  modules.get('alien4cloud-premium-logs', ['a4c-common']).controller('LogSearchPanelController',
  ['$scope', 'searchServiceFactory', '$interval',
  function ($scope, searchServiceFactory, $interval) {
    $scope._ = _;
    $scope.facetFilters = [{term: 'level', facet: ['INFO', 'WARN', 'ERROR' ]}];
    $scope.defaultFilters = {level: ['INFO', 'WARN', 'ERROR' ]};

    $scope.additionalQueryConfigurations = {};

    $scope.onFromDateSet = function (fromDate) {
      $scope.additionalQueryConfigurations.fromDate = fromDate;
    };

    $scope.onToDateSet = function (toDate) {
      $scope.additionalQueryConfigurations.toDate = toDate;
    };

    /*trigger a new search, when params are changed*/
    $scope.doSearch = function () {
      var objectFilters = {};
      $scope.facetFilters.forEach(function (filter) {
        filter = filter || {};
        if (!(filter.term in objectFilters)) {
          // First time the key is present set to the value in filter
          objectFilters[filter.term] = filter.facet;
        } else {
          // Merge otherwise
          objectFilters[filter.term].push.apply(objectFilters[filter.term], filter.facet);
        }
      });
      //add deploymentId as a filter
      _.set(objectFilters, 'deploymentId', [$scope.deploymentId]);
      // if set, add instanceId as a filter
      if (!_.undefined($scope.instanceId) && !_.isNull($scope.instanceId)) {
        _.set(objectFilters, 'instanceId', [$scope.instanceId]);
      }
      // if set, add executionId as a filter
      if (!_.undefined($scope.executionId) && !_.isNull($scope.executionId)) {
        _.set(objectFilters, 'executionId', [$scope.executionId]);
      }
      // if set, add taskId as a filter
      console.log("$scope.taskId: " + $scope.taskId);
      if (!_.undefined($scope.taskId) && !_.isNull($scope.taskId)) {
        _.set(objectFilters, 'taskId', [$scope.taskId]);
      }

      $scope.queryProvider.filters = objectFilters;
      $scope.searchService.search(
        undefined,
        {
          fromDate: $scope.additionalQueryConfigurations.fromDate,
          toDate: $scope.additionalQueryConfigurations.toDate
        },
        undefined);
    };
    //on search completed
    var onSearchCompleted = function (searchResult) {
      if (_.undefined(searchResult.error)) {
        if(_.undefined(_.get(searchResult, 'data.facets.level'))) {
          // We add the log levels as static facets in order to allow to set multiple of them.
          // do not add active filters to facets
          var staticLevelFacets = [];
          _.each([{facetValue: 'DEBUG', count: '?'}, {facetValue: 'INFO', count: '?'}, {facetValue: 'WARN', count: '?'}, {facetValue: 'ERROR', count: '?'}], function(staticLevelFacet) {
            _.each($scope.facetFilters, function(filter) {
              if(filter.term === 'level') {
                if(filter.facet.indexOf(staticLevelFacet.facetValue) === -1) {
                  staticLevelFacets.push(staticLevelFacet);
                }
              }
            });
          });
          if (_.undefined(_.get(searchResult, 'data.facets'))) {
            searchResult.data.facets = {};
          }
          searchResult.data.facets.level = staticLevelFacets;
        }


        $scope.searchResult = searchResult.data;
        $scope.onSearch({
          searchConfig: {
            result: searchResult.data,
            service: $scope.searchService
          }
        });
      } else {
        console.log('error when searching...', searchResult.error);
      }
    };

    // Getting full search result from /data folder
    $scope.queryProvider = {
      query: '',
      onSearchCompleted: onSearchCompleted
    };

    $scope.searchService = searchServiceFactory('rest/latest/deployment/logs/search', false, $scope.queryProvider, 50, 10, undefined, undefined, $scope.additionalQueryConfigurations);
    $scope.searchService.filtered(true);

    //watch over deploymentId key. trigger search if not null and different from the oldValue
    var initialized=false;
    $scope.$watch('deploymentId', function(newValue) {
      if(!initialized && _.defined(newValue)){
        $scope.doSearch();
        initialized=true;
      }
    });

    $scope.refreshRates = [
      {'label': '1 s', 'value': 1000},
      {'label': '2 s', 'value': 2000},
      {'label': '5 s', 'value': 5000},
      {'label': '10 s', 'value': 10000}
    ];

    $scope.refreshRate = $scope.refreshRates[1];

    $scope.toggleTailLog = function () {
      if ($scope.tailLog) {
        $interval.cancel($scope.tailLog);
        $scope.onAutoRefreshToggled({enabled: false});
        delete $scope.tailLog;
      } else {
        $scope.tailLog = $interval(function () {
          $scope.doSearch();
        }, $scope.refreshRate.value);
        $scope.onAutoRefreshToggled({enabled: true});
      }
    };

    $scope.refreshTailLogFrequency = function () {
      if ($scope.tailLog) {
        $interval.cancel($scope.tailLog);
        $scope.tailLog = $interval(function () {
          $scope.doSearch();
        }, $scope.refreshRate.value);
      }
    };

    $scope.$on('$destroy', function () {
      if ($scope.tailLog) {
        $interval.cancel($scope.tailLog);
      }
    });

    $scope.advancedSearch = false;

    $scope.toggleAdvancedSearch = function () {
      $scope.advancedSearch = !$scope.advancedSearch;
    };
    
    $scope.getQueryProviderJson = function () {
      var req = _.merge($scope.additionalQueryConfigurations, $scope.queryProvider);
      return angular.toJson(req);
    }

  }]);
});
