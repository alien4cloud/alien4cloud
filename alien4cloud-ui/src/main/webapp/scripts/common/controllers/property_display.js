define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  require('scripts/common/services/properties_services');

  modules.get('a4c-common', ['pascalprecht.translate']).controller('PropertiesCtrl', ['$scope', 'propertiesServices', '$translate',
    function($scope, propertiesServices, $translate) {

      $scope.propertySave = function(data, unit) {
        delete $scope.unitError;
        if (_.undefined(data) || data.toString() === '') {
          data = null;
        } else if (_.defined($scope.definitionObject.units)) {
          if (_.undefined(unit)) {
            unit = $scope.definitionObject.uiUnit;
          }
          data += ' ' + unit;
        }
        // check constraint here
        var propertyRequest = {
          propertyDefinition: $scope.definition,
          propertyValue: data
        };
        var saveResult = $scope.onSave(propertyRequest);
        // If the callback return a promise
        if (_.defined(saveResult) && _.defined(saveResult.then)) {
          return saveResult.then(function(saveResult) {
            if (saveResult.error !== null) {
              // Constraint error display + translation
              var constraintInfo = saveResult.data;
              // Error message handled by x-editable
              if (saveResult.error.code === 800) {
                return $translate('ERRORS.' + saveResult.error.code + '.' + constraintInfo.name, constraintInfo);
              } else {
                return $translate('ERRORS.' + saveResult.error.code, constraintInfo);
              }
            }
          });
        }
      };

      $scope.saveUnit = function(unit) {
        $scope.definitionObject.uiUnit = unit;
        if (_.defined($scope.definitionObject.uiValue)) {
          var savePromise = $scope.propertySave($scope.definitionObject.uiValue, unit);
          if (_.defined(savePromise)) {
            savePromise.then(function(error) {
              if (_.defined(error)) {
                $scope.unitError = error;
              }
            });
          }
        }
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
      var isReset = false;

      $scope.init = function() {
        $scope.$watch('propertyValue', function() {
          $scope.initScope();
          if (isReset) { // could be an initScope after a reset property
            $scope.propertySave($scope.definitionObject.uiValue, $scope.definitionObject.uiUnit);
          }
        }, true);

        $scope.$watch('definition', function() {
          $scope.initScope();
        }, true);
        $scope.initScope();
      };

      $scope.initScope = function() {
        // Define properties
        if (!_.defined($scope.definition)) {
          return;
        }
        // Now a property is an AbstractPropertyValue : (Scalar or Function)
        var shownValue = $scope.propertyValue;
        if (_.defined($scope.propertyValue) && $scope.propertyValue.definition === false) {
          if ($scope.propertyValue.hasOwnProperty('value')) {
            // Here handle scalar value
            shownValue = $scope.propertyValue.value;
          } else if ($scope.propertyValue.hasOwnProperty('function') && $scope.propertyValue.hasOwnProperty('parameters') && $scope.propertyValue.parameters.length > 0) {
            shownValue = $scope.propertyValue.function + ': ' + _($scope.propertyValue.parameters).toString();
          }
        }

        // handeling default value
        shownValue = shownValue || $scope.definition.default;
        $scope.definitionObject.hasDefaultValue = _.defined($scope.definition.default);

        // Second phase : regarding constraints
        if (_.defined($scope.definition.constraints)) {
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

        var splitScalarUnitValue = function(upperCase) {
          if (_.defined(shownValue)) {
            var shownValueTokens = shownValue.split(/\s+/);
            $scope.definitionObject.uiValue = shownValueTokens[0];
            if (upperCase) {
              $scope.definitionObject.uiUnit = shownValueTokens[1].toUpperCase();
            } else {
              $scope.definitionObject.uiUnit = shownValueTokens[1].toLowerCase();
            }
          } else {
            $scope.definitionObject.uiUnit = $scope.definitionObject.units[0];
            $scope.definitionObject.uiValue = shownValue;
          }
        };

        // Second phase : regardless constraints
        switch ($scope.definition.type) {
          case 'boolean':
            $scope.definitionObject.uiName = 'checkbox';
            if(_.undefined(shownValue)) {
              $scope.definitionObject.uiValue = false;
            } else if(typeof shownValue === 'boolean') {
              $scope.definitionObject.uiValue = shownValue;
            } else {
              $scope.definitionObject.uiValue = (shownValue === 'true');
            }
            break;
          case 'timestamp':
            $scope.definitionObject.uiName = 'date';
            $scope.definitionObject.uiValue = shownValue;
            break;
          case 'scalar-unit.size':
            $scope.definitionObject.uiName = 'scalar-unit';
            $scope.definitionObject.units = ['B', 'KB', 'KIB', 'MB', 'MIB', 'GB', 'GIB', 'TB', 'TIB'];
            splitScalarUnitValue(true);
            break;
          case 'scalar-unit.time':
            $scope.definitionObject.uiName = 'scalar-unit';
            $scope.definitionObject.units = ['d', 'h', 'm', 's', 'ms', 'us', 'ns'];
            splitScalarUnitValue(false);
            break;
          default:
            $scope.definitionObject.uiName = 'string';
            $scope.definitionObject.uiValue = shownValue;
            $scope.definitionObject.uiPassword = $scope.definition.password;
            break;
        }

        // Phase one valid or not ?
        if (!_.isEmpty($scope.definitionObject)) {
          return $scope.definitionObject;
        }
      };

      /** Reset the property to the default value if any */
      $scope.resetProperty = function resetPropertyToDefault() {
        isReset = true;
        if (UTILS.isDefinedAndNotNull($scope.propertyValue)) {
          $scope.propertyValue.value = $scope.definition.default; // if same value affected, no watch applied
        }
      };

      // Init managed property
      $scope.init();
    }
  ]); // controller
}); // define
