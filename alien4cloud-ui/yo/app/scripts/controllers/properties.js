/* global UTILS */

'use strict';

angular.module('alienUiApp').controller('PropertiesCtrl', ['$scope', 'propertiesServices', '$translate', '$q',
  function($scope, propertiesServices, $translate, $q) {
    var initialValue = $scope.propertyValue;

    $scope.propertySave = function(data) {
      if (UTILS.isUndefinedOrNull(data) || data.toString() === '') {
        data = null;
      }
      if(data === initialValue) {
        return;
      }
      // check constraint here
      var saveDefer = $q.defer();
      var propertyRequest = {
        propertyDefinition: $scope.definition,
        propertyValue: data
      };
      var saveResult = $scope.onSave(propertyRequest);
      // If the callback return a promise
      if (UTILS.isDefinedAndNotNull(saveResult) && UTILS.isDefinedAndNotNull(saveResult.then)) {
        saveResult.then(function(saveResult) {
          if (saveResult.error !== null) {
            // Constraint error display + translation
            var constraintInfo = saveResult.data;
            // Error message handled by x-editable
            if (saveResult.error.code === 800) {
              saveDefer.resolve($translate('ERRORS.' + saveResult.error.code + '.' + constraintInfo.name, constraintInfo));
            } else {
              saveDefer.resolve($translate('ERRORS.' + saveResult.error.code, constraintInfo));
            }
          } else {
            saveDefer.resolve(null); // no errors, check then on the promise
          }
        });
      } else {
        saveDefer.resolve(null);
      }
      return saveDefer.promise;
    };

    // specific wrapper for boolean type to handle "css checkbox"
    $scope.propertySaveBoolean = function(propertyValue) {
      $scope.propertySave(propertyValue);
      $scope.definitionObject.uiValue = propertyValue;
    };

    /**
     * Return the property constraint identifier to represent it in the UI
     * We can define the représentation regardint the property "type" or "constraint"
     * Managed identifier / constraint :
     *  - validValues => select
     *  - date => date picker (standby)
     *  - boolean => checkbox
     *  - string => input text / password handled
     *  - inRange => slider
     */
    $scope.definitionObject = {};

    $scope.initScope = function() {
      // Define properties
      if(!UTILS.isDefinedAndNotNull($scope.definition)) {
        return;
      }
      var shownValue = $scope.propertyValue || $scope.definition.default;

      // Second phase : regarding constraints
      if (UTILS.isDefinedAndNotNull($scope.definition.constraints)) {
        for (var i = 0; i < $scope.definition.constraints.length; i++) {

          if ($scope.definition.constraints[i].hasOwnProperty('validValues')) {
            $scope.definitionObject.uiName = 'select';
            $scope.definitionObject.uiValue = shownValue;
            $scope.definitionObject.uiSelectValues = $scope.definition.constraints[i].validValues;
            return $scope.definitionObject;
          }

          if ($scope.definition.constraints[i].hasOwnProperty('inRange')) {
            $scope.definitionObject.uiName = 'range';
            $scope.definitionObject.uiValue = shownValue;
            $scope.definitionObject.uiValueMax = $scope.definition.constraints[i].rangeMaxValue;
            $scope.definitionObject.uiValueMin = $scope.definition.constraints[i].rangeMinValue;
            return $scope.definitionObject;
          }

        }
      }

      // Second phase : regardless constraints
      switch ($scope.definition.type) {
        case 'boolean':
          $scope.definitionObject.uiName = 'checkbox';
          $scope.definitionObject.uiValue = shownValue === 'true' ? true : false;
          break;
        case 'timestamp':
          $scope.definitionObject.uiName = 'date';
          $scope.definitionObject.uiValue = shownValue;
          break;
        default:
          $scope.definitionObject.uiName = 'string';
          $scope.definitionObject.uiValue = shownValue;
          $scope.definitionObject.uiPassword = $scope.definition.password;
          break;
      }

      // Phase one valid or not ?
      if (!UTILS.isObjectEmpty($scope.definitionObject)) {
        return $scope.definitionObject;
      }

      return null;
    };

    // Init managed property
    $scope.initScope();
  }
]);
