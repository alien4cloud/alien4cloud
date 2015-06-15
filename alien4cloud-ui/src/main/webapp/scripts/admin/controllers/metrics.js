define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  require('scripts/admin/metrics/metrics_service');

  // register the state to access the metrics
  states.state('admin.metrics', {
    url: '/metrics',
    templateUrl: 'views/admin/metrics.html',
    controller: 'MetricsCtrl',
    menu: {
      id: 'am.admin.metrics',
      key: 'NAVADMIN.MENU_METRICS',
      state: 'admin.metrics',
      icon: 'fa fa-tachometer',
      priority: 600
    }
  });

  modules.get('alien4cloud-admin').controller('MetricsCtrl', [
    '$scope', 'metricsService', 'healthCheckService', 'threadDumpService',
    function ($scope, metricsService, healthCheckService, threadDumpService) {
      $scope.refresh = function() {
        healthCheckService.check().then(function(promise) {
          $scope.healthCheck = promise;
        },function(promise) {
          $scope.healthCheck = promise.data;
        });

        $scope.metrics = metricsService.get();

        $scope.metrics.$get({}, function(items) {
          $scope.servicesStats = {};
          $scope.cachesStats = {};
          _.each(items.timers, function(value, key) {
            if (key.indexOf('web.rest') !== -1 || key.indexOf('service') !== -1) {
              $scope.servicesStats[key] = value;
            }

            if (key.indexOf('net.sf.ehcache.Cache') !== -1) {
              // remove gets or puts
              var index = key.lastIndexOf('.');
              var newKey = key.substr(0, index);

              // Keep the name of the domain
              index = newKey.lastIndexOf('.');
              $scope.cachesStats[newKey] = {
                'name': newKey.substr(index + 1),
                'value': value
              };
            }
          });
        });
      };

      $scope.refresh();

      $scope.threadDump = function() {
        threadDumpService.dump().then(function(data) {
          $scope.threadDump = data;

          $scope.threadDumpRunnable = 0;
          $scope.threadDumpWaiting = 0;
          $scope.threadDumpTimedWaiting = 0;
          $scope.threadDumpBlocked = 0;

          _.each(data, function(value) {
            if (value.threadState === 'RUNNABLE') {
              $scope.threadDumpRunnable += 1;
            } else if (value.threadState === 'WAITING') {
              $scope.threadDumpWaiting += 1;
            } else if (value.threadState === 'TIMED_WAITING') {
              $scope.threadDumpTimedWaiting += 1;
            } else if (value.threadState === 'BLOCKED') {
              $scope.threadDumpBlocked += 1;
            }
          });

          $scope.threadDumpAll = $scope.threadDumpRunnable + $scope.threadDumpWaiting +
          $scope.threadDumpTimedWaiting + $scope.threadDumpBlocked;

        });
      };

      $scope.getLabelClass = function(threadState) {
        if (threadState === 'RUNNABLE') {
          return 'label-success';
        } else if (threadState === 'WAITING') {
          return 'label-info';
        } else if (threadState === 'TIMED_WAITING') {
          return 'label-warning';
        } else if (threadState === 'BLOCKED') {
          return 'label-danger';
        }
      };
    }
  ]);
});
