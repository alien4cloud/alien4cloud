'use strict';

angular.module('alienUiApp').controller('TopologyTemplateCtrl', ['$scope', '$resource', 'topologyTemplate', 'topologyTemplateService', '$translate',
  function($scope, $resource, topologyTemplateResult, topologyTemplateService, $translate) {

    $scope.topologyTemplate = topologyTemplateResult.data;
    $scope.topologyId = $scope.topologyTemplate.topologyId;
    $scope.topologyTemplateId = $scope.topologyTemplate.id;

    $scope.updateTopologyTemplate = function(fieldName, fieldValue) {
      var topologyTemplateUpdateRequest = {};
      topologyTemplateUpdateRequest[fieldName] = fieldValue;
      return topologyTemplateService.put({
        topologyTemplateId: $scope.topologyTemplateId
      }, angular.toJson(topologyTemplateUpdateRequest), undefined).$promise.then(
        function() {
          // Success
        }, function(errorResponse) {
          // Error
          return $translate('ERRORS.' + errorResponse.data.error.code);
        }
      );
    };
  }]);
