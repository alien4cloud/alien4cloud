define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  modules.get('a4c-metas').controller('MetaPropertiesCtrl', ['$scope', 'propertiesServices', '$translate', 'metapropConfServices', '$resource',
    function($scope, propertiesServices, $translate, metapropConfServices, $resource) {
      $scope._ = _;
      if ($scope.collapsable === undefined) {
        $scope.collapsable = true;
      }

      if(_.undefined($scope.titled)){
        $scope.titled = true;
      }

      function loadMetaProperties() {
        var request = {
          'query': '',
          'filters': {
            target: [$scope.propertiesType]
          },
          'from': 0,
          'size': 5000000 // get all in a single call as we don't have pagination feature here.
        };

        metapropConfServices.search([], angular.toJson(request), function(result) {
          $scope.properties = result.data.data;
        });
      }
      loadMetaProperties();

      var upsertService = $resource($scope.resturl, {}, {
        'upsert': {
          method: 'POST',
          isArray: false,
          headers: {
            'Content-Type': 'application/json; charset=UTF-8'
          }
        }
      });

      /* Call the appropriate service */
      $scope.updateProperty = function(type, propertyDefinitionId, value) {
        var updatePropertyObject = {
          'definitionId': propertyDefinitionId,
          'value': value
        };
        return upsertService.upsert($scope.params,
          angular.toJson(updatePropertyObject), function(response) {
          if (!response.error) {
            if(_.undefined($scope.target.metaProperties)) {
              $scope.target.metaProperties = {};
            }
            $scope.target.metaProperties[updatePropertyObject.definitionId] = updatePropertyObject.value;
          }
        }).$promise;
      };

      /* Return the current value */
      $scope.getPropertyValue = function(metaPropId) {
        if(_.defined($scope.target.metaProperties)) {
          return $scope.target.metaProperties[metaPropId];
        }
        return null;
      };

    }
  ]);
}); // define
