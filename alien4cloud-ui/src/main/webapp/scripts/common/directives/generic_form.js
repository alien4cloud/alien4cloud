define(function(require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('scripts/common/filters/strings');
  require('scripts/common/services/properties_services');

  var FORMS = {};

  FORMS.debugEnabled = false;

  modules.get('a4c-common').directive('genericForm', ['$filter', 'toaster', '$compile', '$interval', function($filter, toaster, $compile, $interval) {
    return {
      restrict: 'E',
      scope: {
        rootName: '=?',
        rootObject: '=?',
        formTitle: '@',
        type: '=',
        /* Form styling options */
        formStyle: '@',
        formClass: '@',
        labelSize: '=',
        /* Flags which modify form behavior */
        isReadOnly: '=',
        isRemovable: '=',
        isCancelable: '=',
        showTree: '=',
        showPath: '=',
        automaticSave: '=',
        partialUpdate: '=',
        useXeditable: '=',
        configuration: '=?',
        /* Callbacks */
        suggest: '&',
        save: '&',
        cancel: '&',
        remove: '&'
      },
      templateUrl: 'views/generic-form/generic_form_template.html',
      link: function(scope, element) {
        FORMS.initGenericForm(scope, toaster, $filter, element);

        scope.deleteData = function() {
          scope.remove();
        };

        // Watch active path to see if i'm concerned by path change
        scope.$watchCollection('configuration.activePath', function() {
          var active = scope.configuration.formStyle !== 'tree' || scope.configuration.activePath.length === 0;
          if (active !== scope.isActivePath) {
            scope.isActivePath = active;
          }
        });

        scope.activePathChangeTo = function(index) {
          // User clicked on root
          if (index === -1) {
            scope.configuration.activePath.length = 0;
            scope.configuration.activeLabelPath.length = 0;
          }
          // If the user clicked on the last path element, do not change anything
          if (index < scope.configuration.activePath.length - 1) {
            var numberToRemove = scope.configuration.activePath.length - index - 1;
            scope.configuration.activePath.splice(index + 1, numberToRemove);
            scope.configuration.activeLabelPath.splice(index + 1, numberToRemove);
          }
        };
        FORMS.initComplexFormScope(scope);
        $interval(function() {
          var formContainerElement = element.find('.genericformpropertiescontainer');
          switch (scope.type._type) {
            case 'complex':
              FORMS.initComplexProperties(scope, scope.type, formContainerElement, $compile);
              break;
            default :
              FORMS.initOthers(scope, scope.type, formContainerElement, $compile);
              break;
          }
        }, 0, 1);
      }
    };
  }]);

  FORMS.initOthers = function(scope, type, element, $compile) {
    var mapElement = FORMS.elementsFactory(type._type, false, scope.configuration.formStyle);
    var newScope = scope.$new();
    newScope.path = scope.path;
    newScope.labelPath = scope.labelPath;
    newScope.propertyName = scope.rootName;
    newScope.propertyType = scope.type;
    element.append($compile(mapElement[0])(newScope));
  };

  modules.get('a4c-common').directive('updateForm', ['$filter', 'toaster', '$compile', '$interval', function($filter, toaster, $compile, $interval) {
    return {
      restrict: 'E',
      scope: {
        rootObject: '=',
        type: '=',
        labelSize: '=',
        suggest: '&',
        save: '&'
      },
      templateUrl: 'views/generic-form/simple_generic_form_template.html',
      link: function(scope, element) {
        scope.automaticSave = true;
        scope.partialUpdate = true;
        FORMS.initGenericForm(scope, toaster, $filter, element);
        $interval(function() {
          FORMS.initComplexProperties(scope, scope.type, element.find('.genericformpropertiescontainer'), $compile);
        }, 0, 1);
      }
    };
  }]);

  modules.get('a4c-common').directive('createFormModal', ['$filter', 'toaster', '$interval', '$compile', function($filter, toaster, $interval, $compile) {
    return {
      restrict: 'E',
      scope: {
        rootObject: '=',
        formTitle: '@',
        type: '=',
        labelSize: '=',
        suggest: '&',
        save: '&',
        cancel: '&'
      },
      templateUrl: 'views/generic-form/create_form_modal_template.html',
      link: function(scope, element) {
        scope.automaticSave = false;
        scope.partialUpdate = false;
        scope.useXeditable = false;
        FORMS.initGenericForm(scope, toaster, $filter, element);
        $interval(function() {
          FORMS.initComplexProperties(scope, scope.type, element.find('.genericformpropertiescontainer'), $compile);
        }, 0, 1);
      }
    };
  }]);

  FORMS.initGenericForm = function(scope, toaster, $filter, element) {
    if (_.undefined(scope.rootObject)) {
      if (scope.type._type === 'array') {
        scope.rootObject = [];
      } else {
        scope.rootObject = {};
      }
    }
    if (_.undefined(scope.rootName)) {
      scope.rootName = 'GENERIC_FORM.ROOT';
    }
    scope.path = [];
    scope.labelPath = [];
    if (_.undefined(scope.configuration)) {
      scope.configuration = {};
    }
    scope.configuration.activePath = [];
    scope.configuration.activeLabelPath = [];
    scope.configuration.validationStatuses = {};
    scope.configuration.formStyle = scope.formStyle || 'tree';
    scope.configuration.automaticSave = scope.automaticSave;
    scope.configuration.partialUpdate = scope.partialUpdate;
    scope.configuration.showErrors = false;
    scope.configuration.showErrorsAlert = false;
    scope.configuration.labelSize = scope.labelSize || 20;
    scope.configuration.rootElement = element;
    scope.configuration.isReadOnly = scope.isReadOnly;
    if (_.defined(scope.useXeditable)) {
      scope.configuration.useXeditable = scope.useXeditable;
    } else {
      scope.configuration.useXeditable = true;
    }
    var saveCallback = scope.save;
    scope.saveAction = function(object) {
      if (_.undefined(scope.configuration.toBeSaved) || !scope.configuration.partialUpdate) {
        scope.configuration.toBeSaved = object;
      } else {
        scope.configuration.toBeSaved = _.merge(object, scope.configuration.toBeSaved);
      }
      scope.configuration.showErrors = true;
      for (var id in scope.configuration.validationStatuses) {
        if (scope.configuration.validationStatuses.hasOwnProperty(id) && !scope.configuration.validationStatuses[id]) {
          scope.configuration.showErrorsAlert = true;
          return;
        }
      }
      var savePromise = saveCallback({
        object: scope.configuration.toBeSaved
      });
      var cleanUpAfterSave = function() {
        delete scope.configuration.toBeSaved;
        delete scope.configuration.validationErrors;
        scope.configuration.showErrorsAlert = false;
      };

      if (_.defined(savePromise)) {
        savePromise.then(function(errors) {
          if (_.defined(errors) && _.defined(errors.error)) {
            // There is a main error
            var resultHtml = $filter('translate')('CLOUDS.ERRORS.' + errors.error.code);
            toaster.pop('error', $filter('translate')('CLOUDS.ERRORS.' + errors.error.code + '_TITLE'), resultHtml, 4000, 'trustedHtml', null);
          } else {
            // There is no main errors, check VALIDATION errors
            if (_.defined(errors) && Object.keys(errors).length > 0 && !errors.hasOwnProperty('error')) {
              scope.configuration.validationErrors = errors;
              scope.configuration.showErrorsAlert = true;
            } else {
              if (!scope.configuration.automaticSave) {
                toaster.pop('success', $filter('translate')(scope.formTitle), $filter('translate')('GENERIC_FORM.SAVE_IS_DONE'), 4000, 'trustedHtml', null);
              }
              cleanUpAfterSave();
            }

          }

        });
      } else {
        if (!scope.configuration.automaticSave) {
          toaster.pop('success', $filter('translate')(scope.formTitle), $filter('translate')('GENERIC_FORM.SAVE_IS_DONE'), 4000, 'trustedHtml', null);
        }
        cleanUpAfterSave();
      }
    };
    scope.closeErrors = function() {
      scope.configuration.showErrorsAlert = false;
    };
    FORMS.initFormSuggest(scope, scope.suggest);
    scope.cancelForm = function() {
      scope.cancel({
        object: scope.rootObject
      });
    };
  };

  modules.get('a4c-common').directive('complexTypeFormLabel', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/complex_type_form_label_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('complexTypeFormLabel', scope, element);
      }
    };
  });

  modules.get('a4c-common').directive('mapTypeFormLabel', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/map_form_label_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('mapTypeFormLabel', scope, element);
      }
    };
  });

  modules.get('a4c-common').directive('arrayTypeFormLabel', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/array_form_label_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('arrayTypeFormLabel', scope, element);
      }
    };
  });

  modules.get('a4c-common').directive('treeForm', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/tree_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('treeForm', scope, element);
        FORMS.initComplexFormScope(scope);
        scope.hasChildren = function() {
          return _.defined(scope.input.value) &&
            angular.isObject(scope.input.value) &&
            Object.keys(scope.input.value).length > 0;
        };
      }
    };
  });

  modules.get('a4c-common').directive('leafForm', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/leaf_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('leafForm', scope, element);
      }
    };
  });

  modules.get('a4c-common').directive('complexTypeForm', ['$compile', '$interval', function($compile, $interval) {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/complex_type_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('complexTypeForm', scope, element);
        FORMS.initComplexFormScope(scope);
        FORMS.initFormSuggest(scope, scope.suggest);
        $interval(function() {
          FORMS.initComplexProperties(scope, scope.propertyType, element.children(), $compile);
        }, 0, 1);
      }
    };
  }]);

  FORMS.initComplexProperties = function(scope, type, element, $compile) {
    scope.childPaths = [];
    scope.childLabelPaths = [];
    scope.childrenElements = [];
    for (var i = 0; i < type._order.length; i++) {
      var childPropertyName = type._order[i];
      var childPropertyType = type._propertyType[childPropertyName];
      var childLabelName = childPropertyType._label || childPropertyName.toString();
      var childPath = FORMS.addKeyToPath(scope.path, childPropertyName);
      var childLabelPath = FORMS.addKeyToPath(scope.labelPath, childLabelName);
      var newElements = FORMS.elementsFactory(childPropertyType._type, false, scope.configuration.formStyle);
      // Build the new scope
      var newScope = scope.$new();
      newScope.path = childPath;
      newScope.labelPath = childLabelPath;
      newScope.propertyName = childPropertyName;
      newScope.propertyType = childPropertyType;
      // Compile and add the newly created elements to their parents
      FORMS.compileAndAppendToParent(scope, newScope, $compile, newElements, element);
      // Add children data to parent
      scope.childrenElements.push.apply(scope.childrenElements, newElements);
      scope.childPaths.push(childPath);
      scope.childLabelPaths.push(childLabelPath);
    }
  };

  modules.get('a4c-common').directive('toscaTypeForm', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/tosca_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('toscaTypeForm', scope, element, true);
        FORMS.initFormSuggest(scope, scope.suggest);
      }
    };
  });

  modules.get('a4c-common').directive('toscaTypeFormLabel', ['propertiesServices', function(propertiesServices) {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/tosca_form_label_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('toscaTypeFormLabel', scope, element, false);
        scope.inputChanged = function(propertyDefinition, propertyValue) {
          if (_.undefined(propertyValue)) {
            FORMS.deleteValueForPath(scope.rootObject, scope.path);
            if (scope.configuration.automaticSave) {
              scope.saveAction(scope.rootObject);
            }
          } else  {
            if (_.isObject(propertyValue) || _.isArray(propertyValue)) {
              // It's an object or an array then don't try to validate constraints
              FORMS.setValueForPath(scope.rootObject, propertyValue, scope.path);
              if (scope.configuration.automaticSave) {
                scope.saveAction(scope.rootObject);
              }
            } else {
              // Only check constraint for primitive properties
              var checkPropertyRequest = {
                'definitionId': scope.propertyName,
                'propertyDefinition': propertyDefinition,
                'value': propertyValue
              };
              return propertiesServices.validConstraints({}, angular.toJson(checkPropertyRequest), function(successResult) {
                if (successResult.error === null) {
                  // No error save the result
                  FORMS.setValueForPath(scope.rootObject, propertyValue, scope.path);
                  if (scope.configuration.automaticSave) {
                    scope.saveAction(scope.rootObject);
                  }
                }
              }).$promise;
            }
          }
        };
      }
    };
  }]);

  modules.get('a4c-common').directive('primitiveTypeForm', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/primitive_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('primitiveTypeForm', scope, element);
        FORMS.initFormSuggest(scope, scope.suggest);
      }
    };
  });

  // Each element display
  modules.get('a4c-common').directive('primitiveTypeFormLabel', ['$filter', function($filter) {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/primitive_form_label_template.html',
      link: function(scope, element) {
        var i;
        FORMS.initFormScope('primitiveTypeFormLabel', scope, element, true);
        scope.input.newValue = scope.input.value;
        if (scope.propertyType._type === 'number') {
          if (_.undefined(scope.propertyType._step)) {
            scope.propertyType._step = 1;
          }
        }

        if (_.defined(scope.propertyType._validValues)) {
          scope.validValues = scope.propertyType._validValues;
          if (scope.propertyType._type === 'number') {
            for (i = 0; i < scope.validValues.length; i++) {
              scope.validValues[i] = Math.floor(scope.validValues[i]);
            }
          } else if (scope.propertyType._type === 'boolean') {
            for (i = 0; i < scope.validValues.length; i++) {
              scope.validValues[i] = scope.validValues[i] === 'true';
            }
          }
        }

        if (_.defined(scope.propertyType._constraints)) {
          scope.constraintsValidators = [];
          for (i = 0; i < scope.propertyType._constraints.length; i++) {
            var constraint = scope.propertyType._constraints[i];
            var constraintName = Object.keys(constraint)[0];
            var constraintReference = constraint[constraintName];
            scope.constraintsValidators.push({
              reference: constraintReference,
              validator: FORMS.constraintFactory(constraintName, $filter)
            });
          }
        }

        scope.toggleBooleanValue = function(yesOrNo) {
          if (scope.input.value === yesOrNo) {
            scope.input.value = null;
          } else {
            scope.input.value = yesOrNo;
          }
          scope.inputChanged();
        };

        var checkValidity = function(value) {
          if (_.defined(scope.constraintsValidators)) {
            for (i = 0; i < scope.constraintsValidators.length; i++) {
              var result = scope.constraintsValidators[i].validator(value, scope.constraintsValidators[i].reference);
              if (result !== true) {
                return result;
              }
            }
          }
        };

        scope.inputCheck = function(value) {
          if (_.defined(value) && value !== '') {
            return checkValidity(value);
          }
        };

        scope.inputChanged = function() {
          FORMS.savePrimitive(scope);
        };

        scope.inputCheckForNonXeditable = function(value) {
          if (scope.input.value) {
            // Once input has changed must delete old input value
            delete scope.input.value;
          }
          var result = checkValidity(value);
          if (result !== true) {
            scope.input.error = result;
            return result;
          } else {
            delete scope.input.error;
          }
        };

        scope.inputChangedForNonXeditable = function() {
          if (_.undefined(scope.input.error)) {
            scope.input.value = scope.input.newValue;
          }
          FORMS.savePrimitive(scope);
        };

        var getSuggest = function(text) {
          if (_.defined(scope.propertyType._suggestion)) {
            return scope.suggest({
              searchConfiguration: scope.propertyType._suggestion,
              text: text
            });
          } else {
            return [];
          }
        };

        scope.suggestion = {
          get: getSuggest,
          waitBeforeRequest: 0,
          minLength: 1
        };
      }
    };
  }]);

  modules.get('a4c-common').directive('arrayTypeForm', ['$compile', '$interval', function($compile, $interval) {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/array_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('arrayTypeForm', scope, element);
        FORMS.initComplexFormScope(scope);
        scope.elements = [];
        scope.childPaths = [];
        scope.childLabelPaths = [];
        scope.index = 0;

        scope.hasElements = function() {
          return scope.elements.length > 0;
        };

        scope.addElementToArray = function() {
          // New elements that will be added to the form
          var newElements = FORMS.elementsFactory(scope.propertyType._contentType._type, true, scope.configuration.formStyle);
          scope.childrenElements.push.apply(scope.childrenElements, newElements);
          var newPath = FORMS.addKeyToPath(scope.path, scope.index);
          var newLabelPath = FORMS.addKeyToPath(scope.labelPath, scope.index.toString());
          // Hold a reference to child's path and element in order to
          // modify it when an index is deleted
          scope.elements.push(newElements);
          scope.childPaths.push(newPath);
          scope.childLabelPaths.push(newLabelPath);
          // Build the new scope to compile newly created elements
          var newScope = scope.$new();
          newScope.path = newPath;
          newScope.labelPath = newLabelPath;
          newScope.propertyName = scope.index;
          newScope.propertyType = scope.propertyType._contentType;
          // Manage delete an entry in the array
          newScope.deleteAction = function(deletedElement) {
            var i;
            // Shift childPaths to the left hand side
            for (i = deletedElement + 1; i < scope.index; i++) {
              scope.childPaths[i][scope.childPaths[i].length - 1] = i - 1;
            }
            FORMS.deleteValueForPath(scope.rootObject, scope.childPaths[deletedElement]);
            scope.childPaths.splice(deletedElement, 1);
            for (i = 0; i < scope.elements[deletedElement].length; i++) {
              scope.elements[deletedElement][i].remove();
            }
            scope.elements.splice(deletedElement, 1);
            if (scope.elements.length === 0) {
              FORMS.deleteValueForPath(scope.rootObject, scope.path);
            }
            scope.index--;
            FORMS.automaticSave(scope);
          };
          FORMS.initFormSuggest(newScope, scope.suggest);
          // Once new scope constructed, compile and add the new elements to the
          // parent containers
          FORMS.compileAndAppendToParent(scope, newScope, $compile, newElements);
          scope.index++;
        };

        // Initialize if array has existing entries
        if (_.defined(scope.input.value)) {
          $interval(function() {
            var inputValueLength = scope.input.value.length;
            for (var i = 0; i < inputValueLength; i++) {
              scope.addElementToArray();
            }
          }, 0, 1);
        }
      }
    };
  }]);

  modules.get('a4c-common').directive('mapTypeForm', ['$compile', '$interval', function($compile, $interval) {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/map_form_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('mapTypeForm', scope, element);
        FORMS.initComplexFormScope(scope);
        scope.key = '';
        scope.entries = {};
        scope.childPaths = [];
        scope.childLabelPaths = [];

        scope.hasEntries = function() {
          return Object.keys(scope.entries).length > 0;
        };

        scope.handleEnter = function(event) {
          if (event.which === 13) {
            scope.addElementToMap();
          }
        };

        scope.addElementToMap = function() {
          var newKey = scope.key;
          if (newKey === '' || newKey in scope.entries) {
            // Do not do anything if the user try to enter an existing or empty
            // key
            return;
          }
          // New elements that will be added to the form
          var newElements = FORMS.elementsFactory(scope.propertyType._contentType._type, true, scope.configuration.formStyle);
          scope.childrenElements.push.apply(scope.childrenElements, newElements);
          // Save newly created elements for delete action
          scope.entries[newKey] = newElements;
          // Build the new scope to compile our elements
          var newScope = scope.$new();
          var newPath = FORMS.addKeyToPath(scope.path, newKey);
          var newLabelPath = FORMS.addKeyToPath(scope.labelPath, newKey);
          scope.childPaths.push(newPath);
          scope.childLabelPaths.push(newLabelPath);
          newScope.path = newPath;
          newScope.labelPath = newLabelPath;
          newScope.propertyName = newKey;
          newScope.propertyType = scope.propertyType._contentType;
          // Manage delete an entry in a map
          newScope.deleteAction = function(deletedElement) {
            for (var i = 0; i < scope.entries[deletedElement].length; i++) {
              scope.entries[deletedElement][i].remove();
            }
            delete scope.entries[deletedElement];
            FORMS.deleteValueForPath(scope.rootObject, newPath);
            if (Object.keys(scope.entries).length === 0) {
              FORMS.deleteValueForPath(scope.rootObject, scope.path);
            }
            FORMS.automaticSave(scope);
          };
          FORMS.initFormSuggest(newScope, scope.suggest);
          // Once new scope constructed, compile and add the new elements to the
          // parent containers
          FORMS.compileAndAppendToParent(scope, newScope, $compile, newElements);
          scope.key = '';
        };

        // Initialize if map has existing entries
        if (_.defined(scope.input.value)) {
          $interval(function() {
            for (var key in scope.input.value) {
              if (scope.input.value.hasOwnProperty(key)) {
                scope.key = key;
                scope.addElementToMap();
              }
            }
          }, 0, 1);
        }
      }
    };
  }]);

  FORMS.savePrimitive = function(scope) {
    if (_.undefined(scope.input.value) || scope.input.value === '') {
      FORMS.deleteValueForPath(scope.rootObject, scope.path);
      scope.input.value = null;
    } else {
      if (_.defined(scope.propertyType._multiplier) && angular.isNumber(scope.input.value)) {
        FORMS.setValueForPath(scope.rootObject, scope.input.value * scope.propertyType._multiplier, scope.path);
      } else {
        FORMS.setValueForPath(scope.rootObject, scope.input.value, scope.path);
      }
    }
    if (angular.isDefined(scope.validateInput)) {
      scope.validateInput();
    }
    FORMS.automaticSave(scope);
  };

  modules.get('a4c-common').directive('abstractTypeForm', ['$compile', '$interval', function($compile, $interval) {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/abstract_form_template.html',
      link: function(scope, element) {
        var listenToImplementationChange = function() {
          scope.selectChangeListener = scope.$watch('propertyType._selectedImplementation.' + scope.propertyName, function(newValue) {
            if (_.defined(newValue) && scope.implementationType !== scope.propertyType._implementationTypes[newValue]) {
              // When selection changed, we must suppress root object
              if (angular.isDefined(scope.implementationType)) {
                // If the implementation is defined, so it means that user changed the implementation we must clean up
                FORMS.deleteValueForPath(scope.rootObject, scope.path);
              }
              scope.implementationType = scope.propertyType._implementationTypes[newValue];
              element.children().children().remove();
              $interval(function() {
                FORMS.initComplexProperties(scope, scope.implementationType, element.children(), $compile);
              }, 0, 1);
            }
          });
        };

        scope.$watch('propertyName', function(newValue, oldValue) {
          if (newValue !== oldValue) {
            scope.selectChangeListener();
            // Refresh the listener
            listenToImplementationChange();
          }
        });

        listenToImplementationChange();

        FORMS.initFormScope('abstractTypeForm', scope, element);
        FORMS.initComplexFormScope(scope);
        FORMS.initFormSuggest(scope, scope.suggest);
      }
    };
  }]);

  modules.get('a4c-common').directive('abstractTypeFormLabel', function() {
    return {
      restrict: 'E',
      scope: FORMS.directiveParameters,
      templateUrl: 'views/generic-form/abstract_form_label_template.html',
      link: function(scope, element) {
        FORMS.initFormScope('abstractTypeFormLabel', scope, element);
        FORMS.initComplexFormScope(scope);
        for (var discriminantProperty in scope.propertyType._implementationTypes) {
          if (scope.propertyType._implementationTypes.hasOwnProperty(discriminantProperty)) {
            var currentImplementation = scope.propertyType._implementationTypes[discriminantProperty];
            currentImplementation._id = discriminantProperty;
            // Try to get discriminant property value to guess the implementation used
            var discriminantPropertyPath = FORMS.addKeyToPath(scope.path, discriminantProperty);
            var discriminantPropertyValue = FORMS.getValueForPath(scope.rootObject, discriminantPropertyPath);
            if (_.undefined(scope.propertyType._selectedImplementation)) {
              scope.propertyType._selectedImplementation = {};
            }
            // The property value can be null, we use the presence of value even if it's null to get the implementation
            if (angular.isDefined(discriminantPropertyValue)) {
              scope.propertyType._selectedImplementation[scope.propertyName] = discriminantProperty;
            } else if (_.undefined(scope.propertyType._selectedImplementation[scope.propertyName])) {
              scope.propertyType._selectedImplementation[scope.propertyName] = currentImplementation._id;
            }
          }
        }

        scope.$watch('propertyName', function(newValue, oldValue) {
          if (newValue !== oldValue) {
            scope.propertyType._selectedImplementation[newValue] = scope.propertyType._selectedImplementation[oldValue];
            delete scope.propertyType._selectedImplementation[oldValue];
          }
        });

        element.on('$destroy', function() {
          delete scope.propertyType._selectedImplementation[scope.propertyName];
        });
      }
    };
  });

  /**
   * Create new elements for complex property, array or map directive. It will
   * return an array of 3 elements. Element 0 : the form to append to the external
   * container, Element 1 : the form to append directly under the parent map,
   * Element 2 : the form to append to the navigation tree.
   */
  FORMS.elementsFactory = function(type, canDelete, formStyle) {
    var newElements = [];
    switch (type) {
      case 'string':
      case 'text':
      case 'number':
      case 'date':
      case 'boolean':
        newElements.push(FORMS.directiveFactory('primitive-type-form', canDelete));
        newElements.push(FORMS.directiveFactory('primitive-type-form-label', canDelete));
        break;
      case 'tosca':
        newElements.push(FORMS.directiveFactory('tosca-type-form', canDelete));
        newElements.push(FORMS.directiveFactory('tosca-type-form-label', canDelete));
        break;
      default:
        newElements.push(FORMS.directiveFactory(type + '-type-form', canDelete));
        newElements.push(FORMS.directiveFactory(type + '-type-form-label', canDelete));
    }
    if (formStyle === 'tree') {
      switch (type) {
        case 'array':
        case 'map':
        case 'abstract':
        case 'complex':
          newElements.push(FORMS.directiveFactory('tree-form', canDelete));
          break;
        default:
          newElements.push(FORMS.directiveFactory('leaf-form', canDelete));
      }
    }
    return newElements;
  };

  /**
   * Compile new elements with new scope and append them to parent
   */
  FORMS.compileAndAppendToParent = function(scope, newScope, $compile, newElements, directiveElement) {
    // Containers for our newly created directive element
    var mainParentElement = FORMS.getMainContainerElement(scope);
    if (_.undefined(directiveElement)) {
      directiveElement = scope.configuration.rootElement.find('.' + scope.complexContainerElementId);
    }
    if (scope.configuration.formStyle === 'tree') {
      var treeParentElement = scope.configuration.rootElement.find('.' + scope.treeContainerElementId);
      // Append newly created directive elements to its respective parent
      treeParentElement.append($compile(newElements[2])(newScope));
    }
    mainParentElement.append($compile(newElements[0])(newScope));
    directiveElement.append($compile(newElements[1])(newScope));
  };

  FORMS.directiveParameters = {
    propertyName: '=',
    propertyType: '=',
    path: '=',
    labelPath: '=',
    rootObject: '=',
    saveAction: '&',
    deleteAction: '&',
    isDeletable: '=',
    suggest: '&',
    configuration: '='
  };

  FORMS.logDirectiveOperation = function(scope, operation) {
    if (FORMS.debugEnabled) {
      console.debug(operation + ' directive [' + scope.idPrefix + '] with name [' + scope.propertyName + '], scope [' + scope.$id + '] and propertyType', scope.propertyType);
    }
  };

  FORMS.getTreeContainerElementId = function(scope) {
    return 'tree' + scope.path.join('') + 'container';
  };

  FORMS.getComplexTypeContainerElementId = function(scope) {
    return 'complextype' + scope.path.join('') + 'container';
  };

  FORMS.getMainContainerElement = function(scope) {
    return scope.configuration.rootElement.find('.genericformtypescontainer');
  };

  FORMS.initOnDestroy = function(scope, element) {
    scope.childrenElements = [];
    var destroyDependencies = function() {
      if (scope.isDestroyed) {
        return;
      }
      FORMS.logDirectiveOperation(scope, 'Destroy');
      // Change active path if currently i'm the active path
      if (_.isEqual(scope.path, scope.configuration.activePath)) {
        scope.configuration.activePath.pop();
        scope.configuration.activeLabelPath.pop();
      }

      for (var i = 0; i < scope.childrenElements.length; i++) {
        scope.childrenElements[i].remove();
      }
      delete scope.childrenElements;
      delete scope.configuration.validationStatuses[scope.pathText];
      if (scope.configuration.validationErrors && scope.configuration.validationErrors[FORMS.valueRequiredError]) {
        // For the moment only error that a field is required exist
        var indexOfError = scope.configuration.validationErrors[FORMS.valueRequiredError].indexOf(scope.labelPath);
        if (indexOfError >= 0) {
          scope.configuration.validationErrors[FORMS.valueRequiredError].splice(indexOfError, 1);
        }
      }
      scope.isDestroyed = true;
    };

    element.on('$destroy', function() {
      scope.$destroy();
    });

    scope.$on('$destroy', function() {
      destroyDependencies();
    });
  };

  FORMS.valueRequiredError = 'GENERIC_FORM.REQUIRED';

  FORMS.applyMultiplierToInput = function(scope) {
    if (_.defined(scope.propertyType._multiplier) && _.defined(scope.input.value) && angular.isNumber(scope.input.value)) {
      scope.input.value = scope.input.value / scope.propertyType._multiplier;
    }
  };

  FORMS.setInputValue = function(scope, path) {
    scope.input.value = FORMS.getValueForPath(scope.rootObject, path);
    FORMS.applyMultiplierToInput(scope);
  };

  FORMS.initPath = function(formName, scope) {
    scope.idPrefix = formName + scope.path.join('');
    scope.pathText = scope.path.join('/');
    scope.labelName = scope.propertyType._label || scope.propertyName.toString();
    // Init and listen on change of the value of the node
    scope.input = {};
    FORMS.setInputValue(scope, scope.path);
  };

  FORMS.initNonDisplayedFormScope = function(formName, scope, element) {
    FORMS.initPath(formName, scope);
    FORMS.logDirectiveOperation(scope, 'Create');
    // Watch path to see if property name might change
    var listenOnInputValueChange = function() {
      var inputValuePathFromRoot = FORMS.getPathAsText('rootObject', scope.path);
      scope.inputValueChangeListener = scope.$watch(inputValuePathFromRoot, function(newValue) {
        if (scope.input.value !== newValue && _.defined(scope.inputValueChangeListener)) {
          scope.input.value = newValue;
          FORMS.applyMultiplierToInput(scope);
          if (angular.isDefined(scope.validateInput)) {
            scope.validateInput();
          }
        }
      });
    };
    scope.$watchCollection('path', function(newPath, oldPath) {
      if (!_.isEqual(newPath, oldPath)) {
        FORMS.logDirectiveOperation(scope, 'Path begins to change as new path is [' + newPath + '] and old one is [' + oldPath + ']');
        scope.propertyName = newPath[newPath.length - 1];
        scope.idPrefix = formName + newPath.join('');
        scope.pathText = newPath.join('/');
        scope.labelName = scope.propertyType._label || scope.propertyName.toString();
        FORMS.setInputValue(scope, newPath);
        if (_.defined(scope.childPaths)) {
          // Propagate path change to all children
          for (var i = 0; i < scope.childPaths.length; i++) {
            scope.childPaths[i][scope.childPaths[i].length - 2] = scope.propertyName;
            scope.childLabelPaths[i][scope.childLabelPaths[i].length - 2] = scope.labelName;
          }
        }
        // Destroy the listener
        scope.inputValueChangeListener();
        delete scope.inputValueChangeListener;
        // Refresh the listener
        listenOnInputValueChange();
        FORMS.logDirectiveOperation(scope, 'Path has changed');
      }
    });
    listenOnInputValueChange();
    FORMS.initOnDestroy(scope, element);
    FORMS.initFormSaveAction(scope, scope.saveAction);
  };

  FORMS.initFormScope = function(formName, scope, element, validateInput) {
    FORMS.initNonDisplayedFormScope(formName, scope, element);
    if (validateInput) {
      scope.validateInput = function() {
        scope.isInputValid = !scope.propertyType._notNull || _.defined(scope.input.value);
        if (scope.configuration.validationErrors && scope.configuration.validationErrors[FORMS.valueRequiredError]) {
          // For the moment only error that a field is required exist
          var indexOfError = scope.configuration.validationErrors[FORMS.valueRequiredError].indexOf(scope.labelPath);
          if (indexOfError >= 0) {
            scope.configuration.validationErrors[FORMS.valueRequiredError].splice(indexOfError, 1);
          }
          if (scope.configuration.validationErrors[FORMS.valueRequiredError].length === 0) {
            delete scope.configuration.validationErrors;
          }
        }
        scope.configuration.validationStatuses[scope.pathText] = scope.isInputValid;
        if (!scope.isInputValid) {

          if (!scope.configuration.validationErrors) {
            scope.configuration.validationErrors = {};
          }
          if (!scope.configuration.validationErrors[FORMS.valueRequiredError]) {
            scope.configuration.validationErrors[FORMS.valueRequiredError] = [];
          }
          scope.configuration.validationErrors[FORMS.valueRequiredError].push(scope.labelPath);
        }
      };
      scope.validateInput();
    } else {
      scope.isInputValid = true;
    }

    // Watch active path to see if i'm concerned by path change
    scope.$watchCollection('configuration.activePath', function(newPath) {
      var active;
      if (scope.configuration.formStyle === 'tree') {
        active = _.isEqual(newPath, scope.path);
      } else {
        active = FORMS.pathContains(scope.path, newPath);
      }
      if (active !== scope.isActivePath) {
        scope.isActivePath = active;
      }
    });
    // Change the current path to mine
    scope.goTo = function() {
      scope.configuration.activePath.length = 0;
      scope.configuration.activePath.push.apply(scope.configuration.activePath, scope.path);
      scope.configuration.activeLabelPath.length = 0;
      scope.configuration.activeLabelPath.push.apply(scope.configuration.activeLabelPath, scope.labelPath);
    };

    if (scope.isDeletable) {
      // Init delete action
      scope.deleteForm = function() {
        scope.deleteAction({
          deletedElement: scope.path[scope.path.length - 1]
        });
      };
    }
  };

  // Search a needle in the haystack
  // Check if the path haystack contain the path needle
  FORMS.pathContains = function(needle, haystack) {
    if (needle.length === 0) {
      return true;
    }
    if (haystack.length < needle.length) {
      return false;
    }
    for (var i = 0; i < needle.length; i++) {
      if (haystack[i] !== needle[i]) {
        return false;
      }
    }
    return true;
  };

  FORMS.initComplexFormScope = function(scope) {
    scope.treeContainerElementId = FORMS.getTreeContainerElementId(scope);
    scope.complexContainerElementId = FORMS.getComplexTypeContainerElementId(scope);
  };

  FORMS.initFormSuggest = function(scope, suggest) {
    scope.suggest = function(searchConfiguration, text) {
      return suggest({
        searchConfiguration: searchConfiguration,
        text: text
      });
    };
  };

  FORMS.initFormSaveAction = function(scope, saveAction) {
    scope.saveAction = function(object) {
      saveAction({
        object: object
      });
    };
  };

  FORMS.directiveFactory = function(directiveName, isDeletable) {
    var elementDeclaration = '<' + directiveName + ' property-name="propertyName"';
    elementDeclaration += ' property-type="propertyType"';
    elementDeclaration += ' path="path"' + ' label-path="labelPath"' + 'root-object="rootObject"' + ' suggest="suggest(searchConfiguration, text)"' + ' is-deletable="' + isDeletable + '" save-action="saveAction(object)" configuration="configuration"';
    if (isDeletable) {
      elementDeclaration += ' delete-action="deleteAction(deletedElement)"';
    }
    elementDeclaration += '></' + directiveName + '>';
    return angular.element(elementDeclaration);
  };

  FORMS.getValueForPath = function(root, path) {
    if (_.isEmpty(path)) {
      return root;
    }
    var objectToSet = root;
    for (var i = 0; i < path.length - 1; i++) {
      if (_.undefined(objectToSet) || !objectToSet.hasOwnProperty(path[i])) {
        return undefined;
      }
      objectToSet = objectToSet[path[i]];
    }
    if (_.undefined(objectToSet)) {
      return undefined;
    } else {
      return objectToSet[path[path.length - 1]];
    }
  };

  FORMS.getPathAsText = function(rootName, path) {
    var pathText = rootName;
    for (var i = 0; i < path.length; i++) {
      if (angular.isNumber(path[i])) {
        pathText += '[' + path[i] + ']';
      } else {
        pathText += '.' + path[i];
      }
    }
    return pathText;
  };

  FORMS.setValueForPath = function(root, value, path) {
    var objectToSet = root;
    for (var i = 0; i < path.length - 1; i++) {
      if (angular.isString(path[i])) {
        if (!objectToSet.hasOwnProperty(path[i]) || _.undefined(objectToSet[path[i]])) {
          if (angular.isNumber(path[i + 1])) {
            // The next one is array so initialize an array
            objectToSet[path[i]] = [];
          } else {
            objectToSet[path[i]] = {};
          }
        }
      } else if (angular.isNumber(path[i])) {
        if (_.undefined(objectToSet[path[i]])) {
          if (angular.isNumber(path[i + 1])) {
            // The next one is array so initialize an array
            objectToSet[path[i]] = [];
          } else {
            objectToSet[path[i]] = {};
          }
        }
      }
      objectToSet = objectToSet[path[i]];
    }
    objectToSet[path[path.length - 1]] = value;
  };

  FORMS.addKeyToPath = function(path, propertyName) {
    var copy = [];
    copy.push.apply(copy, path);
    copy.push(propertyName);
    return copy;
  };

  FORMS.deleteValueForPath = function(root, path) {
    var objectToSet = root;
    for (var i = 0; i < path.length - 1; i++) {
      objectToSet = objectToSet[path[i]];
      if (_.undefined(objectToSet)) {
        return;
      }
    }
    if (_.undefined(objectToSet)) {
      return;
    }
    if (angular.isNumber(path[path.length - 1])) {
      objectToSet.splice(path[path.length - 1], 1);
    } else {
      delete objectToSet[path[path.length - 1]];
    }
  };

  FORMS.automaticSave = function(scope) {
    if (scope.configuration.automaticSave) {
      if (scope.isInputValid) {
        var toBeSaved;
        if (scope.configuration.partialUpdate) {
          toBeSaved = {};
          var value = FORMS.getValueForPath(scope.rootObject, scope.path);
          if (angular.isUndefined(value)) {
            value = null;
          }
          FORMS.setValueForPath(toBeSaved, value, scope.path);
        } else {
          toBeSaved = scope.rootObject;
        }
        scope.saveAction(toBeSaved);
      } else {
        scope.configuration.showErrors = true;
        scope.configuration.showErrorsAlert = true;
      }
    }
  };

  FORMS.constraintFactory = function(name, $filter) {
    switch (name) {
      case 'greaterThan':
        return function(value, reference) {
          if (value <= reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.greaterThan', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'lessThan':
        return function(value, reference) {
          if (value >= reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.lessThan', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'greaterOrEqual':
        return function(value, reference) {
          if (value < reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.greaterOrEqual', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'lessOrEqual':
        return function(value, reference) {
          if (value > reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.lessOrEqual', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'inRange':
        return function(value, reference) {
          var ref = '[ ' + reference.min + ' , ' + reference.max + ' ]';
          if (value < reference.min || value > reference.max) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.inRange', {
              reference: ref
            });
          } else {
            return true;
          }
        };
      case 'length':
        return function(value, reference) {
          if (value.toString().length !== reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.length', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'maxLength':
        return function(value, reference) {
          if (value.toString().length > reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.maxLength', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'minLength':
        return function(value, reference) {
          if (value.toString().length < reference) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.minLength', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      case 'pattern':
        return function(value, reference) {
          var patt = new RegExp(reference);
          if (!patt.test(value.toString())) {
            return $filter('translate')('GENERIC_FORM.VALIDATION_ERROR.pattern', {
              reference: reference
            });
          } else {
            return true;
          }
        };
      default:
        return function() {
          return true;
        };
    }
  };
});
