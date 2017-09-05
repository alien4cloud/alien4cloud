define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');
  require('scripts/_ref/admin/services/audit_service');

  states.state('admin.audit.conf', {
    url: '/conf',
    templateUrl: 'views/_ref/admin/admin_audit_conf.html',
    resolve: {
      auditConfiguration: ['auditService', function(auditService) {
        return auditService.getConfiguration({}, undefined).$promise;
      }]
    },
    controller: 'AuditConfController'
  });

  modules.get('alien4cloud-admin').controller('AuditConfController',
    ['$scope', 'auditService', 'auditConfiguration', '$state', '$translate', 'toaster', function($scope, auditService, auditConfiguration, $state, $translate, toaster) {

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
            $translate.instant('AUDIT.CONFIGURATION.LABEL'),
            $translate.instant('AUDIT.CONFIGURATION.CONFIGURATION_IS_RESETED'),
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
        var currentCategoryConfiguration = _.cloneDeep($scope.configuration.methodsConfiguration[category]);
        var currentCategoryConfigurationLength = currentCategoryConfiguration.length;
        var enableCategory = $scope.categories[category].enabledCount === 0;
        for (var i = 0; i < currentCategoryConfigurationLength; i++) {
          currentCategoryConfiguration[i].enabled = enableCategory;
        }
        auditService.saveConfiguration({
          field: 'audited-methods'
        }, angular.toJson(currentCategoryConfiguration), function success() {
          if (enableCategory) {
            $scope.categories[category].enabledCount = $scope.categories[category].totalCount;
          } else {
            $scope.categories[category].enabledCount = 0;
          }
          $scope.configuration.methodsConfiguration[category] = currentCategoryConfiguration;
        });
      };

      $scope.toggleAuditMethod = function(method) {
        var currentMethodConfiguration = _.cloneDeep(method);
        currentMethodConfiguration.enabled = !currentMethodConfiguration.enabled;
        auditService.saveConfiguration({
          field: 'audited-methods'
        }, angular.toJson([currentMethodConfiguration]), function success() {
          if (currentMethodConfiguration.enabled) {
            $scope.categories[currentMethodConfiguration.category].enabledCount++;
          } else {
            $scope.categories[currentMethodConfiguration.category].enabledCount--;
          }
          method.enabled = currentMethodConfiguration.enabled;
        });
      };

    }]);

});
