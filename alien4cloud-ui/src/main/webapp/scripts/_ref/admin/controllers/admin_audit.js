define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var angular = require('angular');

  require('ng-table');
  require('scripts/_ref/admin/services/audit_service');
  require('scripts/_ref/admin/controllers/admin_audit_conf');
  require('scripts/common/directives/facets');

  // register the state to access the metrics
  states.state('admin.audit', {
    url: '/audit',
    template: '<ui-view/>',
    menu: {
      id: 'am.admin.audit',
      state: 'admin.audit',
      key: 'NAVADMIN.MENU_AUDIT',
      icon: 'fa fa-eye',
      priority: 700
    }
  });
  states.state('admin.audit.log', {
    url: '/log',
    templateUrl: 'views/_ref/admin/admin_audit.html',
    controller: 'AuditController'
  });
  states.forward('admin.audit', 'admin.audit.log');

  modules.get('alien4cloud-admin', ['ngTable']).controller('AuditController', ['$scope', 'auditService', 'ngTableParams', '$filter', '$timeout', '$state',
    function($scope, auditService, ngTableParams, $filter, $timeout, $state) {

      // display configuration
      var timestampFormat = 'medium';
      var DEFAULT_AUDIT_PAGE_SIZE = 10;

      // displayed column
      $scope.columns = [{
        field: 'timestamp',
        visible: true
      }, {
        field: 'userName',
        visible: true
      }, {
        field: 'userFirstName',
        visible: false
      }, {
        field: 'userLastName',
        visible: false
      }, {
        field: 'userEmail',
        visible: false
      }, {
        field: 'category',
        visible: true
      }, {
        field: 'action',
        visible: true
      }, {
        field: 'method',
        visible: true
      }, {
        field: 'responseStatus',
        visible: true
      }, {
        field: 'actionDescription',
        visible: false
      }, {
        field: 'path',
        visible: false
      }, {
        field: 'requestBody',
        visible: false
      }, {
        field: 'requestParameters',
        visible: false
      }, {
        field: 'sourceIp',
        visible: false
      }, {
        field: 'version',
        visible: true
      }, {
        field: 'userAgent',
        visible: false
      }, {
        field: 'alien4CloudUI',
        visible: false
      }];

      var searchRequestObject = {
        'query': '',
        'filters': undefined,
        'fromDate': undefined,
        'toDate': undefined,
        'from': 0,
        'size': DEFAULT_AUDIT_PAGE_SIZE
      };

      $scope.query = '';
      $scope.facetFilters = [];

      // Ignored: A constructor name should start with an uppercase letter.
      $scope.auditTableParam = new ngTableParams({ // jshint ignore:line
        page: 1, // show first page
        count: DEFAULT_AUDIT_PAGE_SIZE // count per page
      }, {
        total: 0, // length of data
        getData: function($defer, params) {

          // getting the exact page
          searchRequestObject.from = ($scope.auditTableParam.page() - 1) * $scope.auditTableParam.count();
          searchRequestObject.size = $scope.auditTableParam.count();

          // audit search
          auditService.search([], angular.toJson(searchRequestObject), function(successResult) {
            // get data & facets
            var data = successResult.data.data;
            var total = successResult.data.totalResults;
            $scope.facets = successResult.data.facets;

            // prepare to dysplay
            prepareTraces(data);
            params.total(total);
            $defer.resolve(data);
          });
        }
      });

      //////////////////////////////////
      // UI utils methods
      //////////////////////////////////

      // format traces befor display
      function prepareTraces(traces) {
        // prepare each "timestamp" field
        traces.forEach(function(trace) {
          trace.timestamp = $filter('date')(trace.timestamp, timestampFormat);
        });
      }

      $scope.goToAuditConfiguration = function() {
        $state.go('admin.audit.conf');
      };

      // update search result table
      function updateSearch(keyword, filters) {

        /*
         Search api expect a json object matching the following pattern:
         {
         'query': 'mysearched',
         'from' : int,
         'to' : int,
         'filters': {'termId1' : ['facetId1'],'termId2' : ['facetId2','facetedId3'] }
         }
         */

        // Convert filter [] filters -> Object
        var objectFilters = {};
        filters.forEach(function(filter) {

          filter = filter || {};
          if (!(filter.term in objectFilters)) {
            // First time the key is present set to the value in filter
            objectFilters[filter.term] = filter.facet;
          } else {
            // Merge otherwise
            objectFilters[filter.term].push.apply(objectFilters[filter.term], filter.facet);
          }
        });

        searchRequestObject.query = keyword;
        searchRequestObject.filters = objectFilters;

        // reload traces table
        $scope.auditTableParam.reload();

      }

      $scope.onChangeFromDate = function(newDate) {
        searchRequestObject.fromDate = newDate;
        doSearch();
      }

      $scope.onChangeToDate = function(newDate) {
        searchRequestObject.toDate = newDate;
        doSearch();
      }

      //////////////////////////////////
      // Search methods
      //////////////////////////////////
      var doSearch = function() {
        // prepare filters
        var allFacetFilters = [];
        allFacetFilters.push.apply(allFacetFilters, $scope.facetFilters);
        updateSearch($scope.searchedKeyword, allFacetFilters);
      };

      $scope.doSearch = doSearch;

    }
  ]);
});
