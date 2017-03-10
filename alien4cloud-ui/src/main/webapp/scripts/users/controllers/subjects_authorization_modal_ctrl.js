define(function (require) {
  'use strict';
  /**
  ** controller for simple subject (such as user and group) authorizations granting modal
  **
  */

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/search_service_factory');
  require('scripts/common/directives/pagination');


  modules.get('a4c-security', ['a4c-search']).controller('SubjectAuthorizationModalCtrl', ['$scope', '$uibModalInstance', 'searchServiceFactory',
    function ($scope, $uibModalInstance, searchServiceFactory) {
      $scope.batchSize = 5;
      $scope.selectedSubjects = [];
      $scope.query = '';

      function getIdFilter(subject){
        var filter = {};
        filter[$scope.idPropertyName] = _.get(subject, $scope.idPropertyName);
        return filter;
      }

      var indexOf = function (selectedSubjects, subject) {
        return _.findIndex(selectedSubjects, getIdFilter(subject));
      };

      var authorizedSubjects = $scope.authorizedSubjects;

      var buildSeachService = function(){
        var url;
        var useParams ;
        var params ;
        var searchConfig = _.isFunction($scope.buildSearchConfig) ? $scope.buildSearchConfig() : null;
        if($scope.customSearchActive) {
          url = _.get(searchConfig, 'url', $scope.defaultSeaUrl);
          useParams = _.get(searchConfig, 'useParams', false);
          params = _.get(searchConfig, 'params', null);
        }else {
          url = $scope.defaultSearchUrl;
          useParams = false;
          params = null;
        }
        $scope.searchService = searchServiceFactory(url, useParams, $scope, $scope.batchSize, null, null, null, params);
        $scope.searchService.search();
      };

      $scope.onSearchCompleted = function (searchResult) {
        $scope.searchedData = searchResult.data;
        $scope.selectedSubjectsInCurrentPage = _.filter($scope.searchedData.data, function (searchedUser) {
          return indexOf($scope.selectedSubjects, searchedUser) >= 0;
        });
      };

      buildSeachService();

      $scope.ok = function () {
        if ($scope.selectedSubjects.length > 0) {
          $uibModalInstance.close({subjects:$scope.selectedSubjects});
        }
      };

      $scope.search = function (event) {
        $scope.selectedSubjects = [];
        $scope.searchService.search();
        event.preventDefault();
      };

      $scope.toggleSelection = function (subject) {
        var indexOfUserInSelected = $scope.selectedSubjectsInCurrentPage.indexOf(subject);
        if (indexOfUserInSelected < 0) {
          $scope.selectedSubjectsInCurrentPage.push(subject);
          $scope.selectedSubjects.push(subject);
        } else {
          $scope.selectedSubjectsInCurrentPage.splice(indexOfUserInSelected, 1);
          _.remove($scope.selectedSubjects, getIdFilter(subject));
        }
      };

      $scope.isSelected = function (subject) {
        return $scope.selectedSubjectsInCurrentPage.indexOf(subject) >= 0;
      };

      $scope.isAuthorized = function(subject) {
        return indexOf(authorizedSubjects, subject) >= 0;
      };


      $scope.toggleSelectAll = function () {
        // Remove anyway all the elements of the current page from the selected list
        $scope.selectedSubjects = _.filter($scope.selectedSubjects, function (selectedUser) {
          return indexOf($scope.searchedData.data, selectedUser) < 0;
        });
        if ($scope.selectedSubjectsInCurrentPage.length === $scope.searchedData.data.length) {
          $scope.selectedSubjectsInCurrentPage = [];
        } else {
          $scope.selectedSubjectsInCurrentPage = $scope.searchedData.data.slice();
          //remove already authorized subjects
          _.each(authorizedSubjects, function(subject){
            _.remove($scope.selectedSubjectsInCurrentPage, getIdFilter(subject));
          });
          $scope.selectedSubjects = _.concat($scope.selectedSubjects, $scope.selectedSubjectsInCurrentPage);
        }

      };

      $scope.getSelectAllClass = function(){
        var allCurrentPageUsernames = _.map($scope.searchedData.data, $scope.idPropertyName);
        var allSelected = _.map(_.union(authorizedSubjects, $scope.selectedSubjectsInCurrentPage), $scope.idPropertyName);
        var allSelectedInCurrentPage = _.intersection(allSelected, allCurrentPageUsernames);

        if(_.isEmpty(allSelectedInCurrentPage)){
          return 'fa-square-o';
        } else if (_.every(allCurrentPageUsernames, function(subjectId){
          return _.includes(allSelectedInCurrentPage, subjectId);
        })) {
          return 'fa-check-square-o';
        }else {
          return 'fa-minus-square-o';
        }
      };

      $scope.toggleCustomSearch = function(){
        $scope.customSearchActive = !$scope.customSearchActive;
        buildSeachService();
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }
  ]);
});
