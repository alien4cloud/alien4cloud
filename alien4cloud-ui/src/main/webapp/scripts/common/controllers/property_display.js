define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/common/services/properties_services');

  var ComplexPropertyModalCtrl = ['$scope', '$modalInstance', 'formDescriptorServices',
    function($scope, $modalInstance, formDescriptorServices) {
      $scope.configuration = {
        dependencies: $scope.dependencies
      };
      var descriptorQuery = {
        propertyDefinition: $scope.definition,
        dependencies: $scope.dependencies
      };
      //descriptor of the config
      formDescriptorServices.getToscaComplexTypeDescriptor({}, angular.toJson(descriptorQuery), function(result) {
        $scope.formDescription = result.data;
      });

      $scope.save = function(value) {
        $scope.propertySave(value);
      };

      $scope.remove = function() {
        $scope.propertySave(undefined);
        $modalInstance.dismiss(undefined);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ];

  var PropertySuggestionModalCtrl = ['$scope', '$modalInstance',
    function($scope, $modalInstance) {
      $scope.result = '';

      $scope.create = function(result) {
        $modalInstance.close(result);
      };

      $scope.cancel = function() {
        $modalInstance.dismiss('cancel');
      };
    }
  ];


  modules.get('a4c-common', ['pascalprecht.translate']).controller('PropertiesCtrl', ['$scope', 'propertiesServices', '$translate', '$modal', '$timeout', 'propertySuggestionServices',
    function($scope, propertiesServices, $translate, $modal, $timeout, propertySuggestionServices) {
      if (_.undefined($scope.translated)) {
        $scope.translated = false;
      }

      $scope.showLongTextChoice = false;

      $scope.switchLongTextChoice = function(on) {
        $scope.showLongTextChoice = on;
      };

      $scope.switchToLongText = function($event) {
        $scope.isLongText = !$scope.isLongText;
        $timeout(function() {
          angular.element($event.target).prev().trigger('click');
        });
      };

      $scope.suggestion = {
        get: function(text) {
          if (_.defined($scope.definition.suggestionId)) {
            return propertySuggestionServices.get({
              input: text,
              limit: 5,
              suggestionId: $scope.definition.suggestionId
            }).$promise.then(function(result) {
              if (_.defined(result.data)) {
                if (result.data.indexOf(text) < 0) {
                  result.data.push(text);
                }
                return result.data;
              } else {
                return [];
              }
            });
          } else {
            return [];
          }
        },
        waitBeforeRequest: 0,
        minLength: 1
      };

      /* method private to factorise all call to the serve and trigge errors */
      var callSaveService = function(propertyRequest) {
        var saveResult = $scope.onSave(propertyRequest);

        // If the callback return a promise
        if (_.defined(saveResult) && _.defined(saveResult.then)) {
          return saveResult.then(function(saveResult) {
            if (_.defined(saveResult.error)) {
              // Constraint error display + translation
              var constraintInfo = saveResult.data;
              // Error message handled by x-editable
              if (saveResult.error.code === 800) {
                return $translate.instant('ERRORS.' + saveResult.error.code + '.' + constraintInfo.name, constraintInfo);
              } else {
                return $translate.instant('ERRORS.' + saveResult.error.code, constraintInfo);
              }
            }
          });
        }
      };

      $scope.propertySave = function(data, unit) {
        delete $scope.unitError;
        if (_.isBoolean(data)) {
          data = data.toString();
        } else if (_.isEmpty(data)) {
          data = null;
        }

        if (!_.isEmpty(data) && _.defined($scope.definitionObject.units)) {
          if (_.undefined(unit)) {
            unit = $scope.definitionObject.uiUnit;
          }
          data += ' ' + unit;
        }
        // check constraint here
        var propertyRequest = {
          propertyName: $scope.propertyName,
          propertyDefinition: $scope.definition,
          propertyValue: data
        };

        if (_.defined($scope.definition.suggestionId) && _.defined(data) && data !== null) {
          return propertySuggestionServices.get({
            input: data,
            limit: 5,
            suggestionId: $scope.definition.suggestionId
          }).$promise.then(function(suggestionResult) {
            var promise;
            $scope.propertySuggestionData = {
              suggestions: suggestionResult.data,
              propertyValue: data
            };
            if (suggestionResult.data.indexOf(data) < 0) {
              var modalInstance = $modal.open({
                templateUrl: 'propertySuggestionModal.html',
                controller: PropertySuggestionModalCtrl,
                scope: $scope
              });
              promise = modalInstance.result.then(function(modalResult) {
                if (suggestionResult.data.indexOf(modalResult) < 0) {
                  propertySuggestionServices.add([], {
                    suggestionId: $scope.definition.suggestionId,
                    value: modalResult
                  }, null);
                }
                propertyRequest.propertyValue = modalResult;
                return callSaveService(propertyRequest);
              }, function() {
                return $translate.instant('CANCELLED');
              });
            } else {
              promise = callSaveService(propertyRequest);
            }
            return promise;
          });
        } else {
          return callSaveService(propertyRequest);
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

      $scope.saveReset = function(resetValue) {
        var resetUnit = null;
        if (_.defined($scope.definitionObject.units) && !_.undefined(resetValue)) {
          // reset value de la forme : "VALUE UNIT" > split by space
          var splitedValueUnit = resetValue.split(' ');
          resetValue = splitedValueUnit[0];
          resetUnit = splitedValueUnit[1];
        }
        var savePromise = $scope.propertySave(resetValue, resetUnit);
        if (_.defined(savePromise)) {
          savePromise.then(function(error) {
            if (_.defined(error)) {
              $scope.unitError = error;
            }
          });
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

      $scope.init = function() {
        $scope.$watch('propertyValue', function() {
          $scope.initScope();
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
            //should not edit a function
            $scope.editable = false;
          }
        }

        // handling default value
        shownValue = shownValue || $scope.definition.default;
        $scope.definitionObject.hasDefaultValue = _.defined($scope.definition.default);

        // merge the constraints from the definition and from the type
        var constraints = [];
        if (_.defined($scope.definition.constraints)) {
          constraints = $scope.definition.constraints;
        }
        if (_.defined($scope.propertyType) && _.defined($scope.propertyType.constraints)) {
          _.concat(constraints, $scope.propertyType.constraints);
        }

        // Second phase : regarding constraints
        for (var i = 0; i < constraints.length; i++) {
          if (constraints[i].hasOwnProperty('validValues')) {
            $scope.definitionObject.uiName = 'select';
            $scope.definitionObject.uiValue = shownValue;
            $scope.definitionObject.uiSelectValues = constraints[i].validValues;
            return $scope.definitionObject;
          }
          if (constraints[i].hasOwnProperty('inRange')) {
            $scope.definitionObject.uiName = 'string';
            $scope.definitionObject.uiValue = shownValue;
            $scope.definitionObject.uiValueMax = constraints[i].rangeMaxValue;
            $scope.definitionObject.uiValueMin = constraints[i].rangeMinValue;
            return $scope.definitionObject;
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
        $scope.splitScalarUnitValue = splitScalarUnitValue;

        var type = $scope.definition.type;
        if (_.defined($scope.propertyType) && $scope.propertyType.deriveFromSimpleType) {
          type = $scope.propertyType.derivedFrom[0];
        }

        $scope.definitionObject.uiEmpty = false;
        if(_.undefined(shownValue) || _.isEmpty(shownValue)) {
          $scope.definitionObject.uiEmpty = true;
        }

        // Second phase : regardless constraints
        switch (type) {
          case 'boolean':
            $scope.definitionObject.uiName = 'checkbox';
            if (_.undefined(shownValue)) {
              $scope.definitionObject.uiValue = false;
            } else if (typeof shownValue === 'boolean') {
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
          case 'scalar-unit.frequency':
            $scope.definitionObject.uiName = 'scalar-unit';
            $scope.definitionObject.units = ['Hz', 'KHz', 'MHz', 'GHz'];
            splitScalarUnitValue(true);
            break;
          case 'version':
          case 'float':
          case 'integer':
          case 'string':
            $scope.definitionObject.uiName = 'string';
            if(_.defined(shownValue) && _.defined(shownValue.value)) {
              $scope.definitionObject.uiValue = shownValue.value;
            } else {
              $scope.definitionObject.uiValue = shownValue;
            }
            $scope.definitionObject.uiPassword = $scope.definition.password;
            $scope.isLongText = _.defined(shownValue) && typeof shownValue === 'string' && shownValue.indexOf('\n') > -1;
            break;
          default :
            $scope.definitionObject.uiName = 'complex';
            $scope.definitionObject.uiValue = shownValue;
            break;
        }
        // Phase one valid or not ?
        if (!_.isEmpty($scope.definitionObject)) {
          return $scope.definitionObject;
        }
      };

      $scope.openComplexPropertyModal = function() {
        $modal.open({
          templateUrl: 'views/common/property_display_complex_modal.html',
          controller: ComplexPropertyModalCtrl,
          windowClass: 'searchModal',
          scope: $scope
        });
      };

      /** Reset the property to the default value if any */
      $scope.resetProperty = function resetPropertyToDefault() {
        $scope.initScope();
        var defaultValue = null;
        if(_.has($scope.definition, 'default.value')){
          defaultValue = $scope.definition.default.value;
        }else{
          defaultValue = _.get($scope.definition, 'default');
        }
        $scope.saveReset(defaultValue);

        if (_.has($scope.propertyValue, 'value')) {
          $scope.propertyValue.value = defaultValue; // if same value affected, no watch applied
        } else {
          $scope.propertyValue = defaultValue;
        }
      };

      // Init managed property
      $scope.init();
    }
  ]); // controller
}); // define
