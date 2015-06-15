'use strict';

angular.module('alienUiApp').controller(
  'TagConfigurationListCtrl',
  ['$scope', '$state', 'tagConfigurationServices', 'searchServiceFactory', 'formDescriptorServices',
    function($scope, $state, tagConfigurationServices, searchServiceFactory, formDescriptorServices) {

      $scope.tagCreationFormOpened = false;

      $scope.formTitle = 'Tag Configuration';

      $scope.query = '';

      $scope.onSearchCompleted = function(searchResult) {
        $scope.data = searchResult.data;
      };

      $scope.searchService = searchServiceFactory('rest/tagconfigurations/search', false, $scope, 20);

      //first load
      $scope.searchService.search();

      formDescriptorServices.getTagConfigurationDescriptor.get({}, function(success) {
        $scope.objectDefinition = success.data;
      });
      $scope.saveTagConfiguration = function(config) {
        return tagConfigurationServices.save(config).then(function(response) {
          if (response.validationErrors) {
            return tagConfigurationServices.processValidationErrors(response.validationErrors);
          } else {
            $scope.tagCreationFormOpened = false;
            $scope.searchService.search();
          }
        });
      };

      $scope.cancelTagConfigurationCreation = function() {
        $scope.tagCreationFormOpened = false;
      };

      $scope.removeTagConfiguration = function(configId) {
        tagConfigurationServices.remove(configId).then(function() {
          $scope.searchService.search();
        });
      };

      $scope.goToTagConfiguration = function(id) {
        $state.go('admin.metaprops.detail', {id: id});
      };

      $scope.openTagCreationForm = function() {
        $scope.tagCreationFormOpened = true;
      };
    }]);
