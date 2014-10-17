/* global UTILS */

'use strict';

angular.module('alienUiApp').controller(
    'CsarComponentDetailsCtrl',
    ['$scope', '$stateParams', '$state', 'csarService', 'suggestionServices', 'formDescriptorServices',
        function($scope, $stateParams, $state, csarService, suggestionServices, formDescriptorServices) {

          $scope.csarId = $stateParams.csarId;

          $scope.refreshDetails = function() {
            csarService.nodeTypeCRUDDAO.get({
              csarId : $scope.csarId,
              nodeTypeId : $stateParams.nodeTypeId
            }, function(successResult) {
              $scope.nodeType = successResult.data;
            });
          };

          // init details page
          $scope.refreshDetails();

          formDescriptorServices.getNodeTypeFormDescriptor().then(function(result) {
            $scope.objectDefinition = result;
          });

          $scope.suggest = function(searchConfiguration, text) {
            if (angular.isDefined(searchConfiguration) && angular.isDefined(text)) {
              return suggestionServices.getSuggestions(searchConfiguration._index, searchConfiguration._type, searchConfiguration._path, text);
            } else {
              return [];
            }
          };

          $scope.deleteNodeType = function(nodeTypeId) {
            csarService.nodeTypeCRUDDAO.remove({
              csarId : $scope.csarId,
              nodeTypeId : $stateParams.nodeTypeId
            }, function() {
              if(UTILS.isDefinedAndNotNull(nodeTypeId)) {
                $state.go('csardetailnode', { csarId: $scope.csarId, nodeTypeId: nodeTypeId });
              } else {
                $state.go('csardetail', { csarId: $scope.csarId });
              }
            });
          };

          $scope.saveNodeType = function(nodeType) {
            csarService.createNodeType.upload({
              csarId : $scope.csarId
            }, angular.toJson(nodeType), function() {
              if ($stateParams.nodeTypeId === nodeType.id) {
                // User did not change id just refresh
                $scope.refreshDetails();
              } else {
                // Must delete the old one and change path to the new one
                $scope.deleteNodeType('/csars/' + $scope.csarId + '/' + nodeType.id);
              }
            });
          };

          $scope.cancelNodeTypeUpdate = function() {
            $state.go('csardetail', { csarId: $scope.csarId });
          };
        }]);
