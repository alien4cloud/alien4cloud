'use strict';

angular.module('alienUiApp').controller('AuditController', ['$scope', 'auditService', 'ngTableParams', '$filter',
  function($scope, auditService, ngTableParams, $filter) {

    // table configuration
    var searchRequestObject = {
      'query': '',
      'from': 0,
      'size': 1000
    };

    $scope.columns = [{
      title: 'Date',
      field: 'timestamp',
      visible: true
    }, {
      title: 'Username',
      field: 'userName',
      visible: true
    }, {
      title: 'Category',
      field: 'category',
      visible: true
    },{
      title: 'Action',
      field: 'category', // TODO: change for action
      visible: true
    },{
      title: 'Method',
      field: 'method',
      visible: true
    },{
      title: 'Response status',
      field: 'responseStatus',
      visible: true
    }];

    // recover audit search results
    auditService.search([], angular.toJson(searchRequestObject), function(successResult) {
      $scope.rows = successResult.data.data;
      // get categories
      $scope.categories = Object.keys(successResult.data.facets);
      $scope.groupby = 'userName';
    });

    // configure the table
    $scope.auditTableParam = new ngTableParams({
      page: 1, // show first page
      count: 15 // count per page
    }, {
      groupBy: $scope.groupby,
      total: function() {
        return $scope.rows.length;
      }, // length of data
      getData: function($defer, params) {
        var orderedData = params.sorting() ?
          $filter('orderBy')($scope.rows, $scope.auditTableParam.orderBy()) : $scope.rows;
        $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
      }
    });

    // watch for grouping
    $scope.$watch('groupby', function(value) {
      $scope.auditTableParam.settings().groupBy = value;
      console.log('Scope Value', $scope.groupby);
      console.log('Watch value', this.last);
      console.log('new table', $scope.auditTableParam);
      $scope.auditTableParam.reload();
    });

  }
]);
