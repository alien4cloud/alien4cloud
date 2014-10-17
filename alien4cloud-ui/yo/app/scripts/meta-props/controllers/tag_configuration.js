'use strict';

angular.module('alienUiApp').controller('TagConfigurationCtrl', ['$scope', '$stateParams', '$state', 'tagConfigurationServices', 'formDescriptorServices',
  function($scope, $stateParams, $state, tagConfigurationServices, formDescriptorServices) {

    $scope.refreshDetails = function() {
      tagConfigurationServices.get($stateParams.id).then(function(config) {
        $scope.configuration = config;
        $scope.formTitle = config.name;
      });
    };

    $scope.refreshDetails();

    formDescriptorServices.getTagConfigurationDescriptor.get({}, function(success) {
      $scope.objectDefinition = success.data;
    });

    $scope.saveTagConfiguration = function(config) {
      return tagConfigurationServices.save(config).then(function(response) {
        return tagConfigurationServices.processValidationErrors(response.validationErrors);
      });
    };

    $scope.removeTagConfiguration = function() {
      tagConfigurationServices.remove($stateParams.id).then(function() {
        $state.go('admin.metaprops.list');
      });
    };

    $scope.cancelTagConfigurationCreation = function() {
      $state.go('admin.metaprops.list');
    };
  }]);
