'use strict';

angular.module('alienUiApp').controller('AuditConfController',
  ['$scope', 'auditService', 'auditConfiguration', '$state', '$filter', 'toaster', function($scope, auditService, auditConfiguration, $state, $filter, toaster) {

    var refresh = function() {

      $scope.configuration = auditConfiguration.data;

      $scope.categories = {};
      if ($scope.configuration.hasOwnProperty('methodsConfiguration')) {
        for (var category in $scope.configuration.methodsConfiguration) {
          if ($scope.configuration.methodsConfiguration.hasOwnProperty(category)) {
            var totalCount = $scope.configuration.methodsConfiguration[category].length;
            var enabledCount = 0;
            for (var i = 0; i < totalCount; i++) {
              if ($scope.configuration.methodsConfiguration[category][i].enabled) {
                enabledCount++;
              }
            }
            $scope.categories[category] = {
              totalCount: totalCount,
              enabledCount: enabledCount
            };
          }
        }
      }
    };

    refresh();

    $scope.goBackToLog = function() {
      $state.go('admin.audit.log');
    };

    $scope.reset = function() {
      auditService.saveConfiguration({
        field: 'reset'
      }, undefined, function success(response) {
        auditConfiguration.data = response.data;
        refresh();
        toaster.pop(
          'success',
          $filter('translate')('AUDIT.CONFIGURATION.LABEL'),
          $filter('translate')('AUDIT.CONFIGURATION.CONFIGURATION_IS_RESETED'),
          4000, 'trustedHtml', null
        );
      });
    };

    $scope.toggleAudit = function(enabled) {
      if ($scope.configuration.enabled !== enabled) {
        auditService.saveConfiguration({
          field: 'enabled',
          enabled: enabled
        }, undefined, function success() {
          $scope.configuration.enabled = enabled;
        });
      }
    };

    $scope.toggleAuditCategory = function(category) {
      var currentCategoryConfiguration = UTILS.deepCopy($scope.configuration.methodsConfiguration[category]);
      var currentCategoryConfigurationLength = currentCategoryConfiguration.length;
      var enableCategory = $scope.categories[category].enabledCount == 0
      for (var i = 0; i < currentCategoryConfigurationLength; i++) {
        currentCategoryConfiguration[i].enabled = enableCategory;
      }
      auditService.saveConfiguration({
        field: 'audited-methods'
      }, angular.toJson(currentCategoryConfiguration), function success(response) {
        if (enableCategory) {
          $scope.categories[category].enabledCount = $scope.categories[category].totalCount;
        } else {
          $scope.categories[category].enabledCount = 0;
        }
        $scope.configuration.methodsConfiguration[category] = currentCategoryConfiguration;
      });
    };

    $scope.toggleAuditMethod = function(method) {
      var currentMethodConfiguration = UTILS.deepCopy(method);
      currentMethodConfiguration.enabled = !currentMethodConfiguration.enabled;
      auditService.saveConfiguration({
        field: 'audited-methods'
      }, angular.toJson([currentMethodConfiguration]), function success(response) {
        if (currentMethodConfiguration.enabled) {
          $scope.categories[currentMethodConfiguration.category].enabledCount++;
        } else {
          $scope.categories[currentMethodConfiguration.category].enabledCount--;
        }
        method.enabled = currentMethodConfiguration.enabled;
      });
    };

  }]);
