define(function (require) {
  'use strict';
  var modules = require('modules');
  var _ = require('lodash');
  require('scripts/common/directives/empty_place_holder');
  require('scripts/common/filters/reverse');
  require('scripts/common/services/resize_services');
  require('scripts/log/directives/scroll_bottom');


  modules.get('alien4cloud-premium-logs').directive('logSearchDisplay',
      function () {
        return {
          templateUrl: 'views/log/log_search_display.html',
          restrict: 'E',
          controller: 'LogDisplayController',
          scope: {
            searchConfig: '=',
            showDeploymentPaasId: '=',
            autoRefreshEnabled: '<'
          }
        };
      }
  );

  modules.get('alien4cloud-premium-logs').controller('LogDisplayController', ['$scope', '$uibModal', '$window', '$timeout', 'resizeServices',

  function ($scope, $uibModal, $window, $timeout, resizeServices) {
      $scope.columns = [{
        field: 'timestamp',
        visible: true
      }, {
        field: 'level',
        visible: false
      }, {
        field: 'deploymentPaaSId',
        visible: true
      }, {
        field: 'workflowId',
        visible: true
      }, {
        field: 'executionId',
        visible: false
      }, {
        field: 'nodeId',
        visible: true
      }, {
        field: 'instanceId',
        visible: false
       }, {
        field: 'interfaceName',
        visible: false
      }, {
        field: 'operationName',
        visible: false
      }, {
        field: 'type',
        visible: false
      }, {
        field: 'content',
        visible: true
      }];

      if (!_.undefined($scope.showDeploymentPaasId)) {
        $scope.columns.forEach(function (column) {
          if (column.field === 'deploymentPaaSId') {
            column.visible = $scope.showDeploymentPaasId;
          }
        });
      }

      $scope.detailsColumn1 = ['timestamp', 'nodeId', 'workflowId'];
      $scope.detailsColumn2 = ['level', 'interfaceName', 'executionId'];
      $scope.detailsColumn3 = ['type', 'operationName'];

      //////////////////////////////////
      // Modal to show/hide columns
      //////////////////////////////////
      var ModalInstanceCtrl = ['$scope', '$uibModalInstance', 'columns', function ($scope, $uibModalInstance, columns) {
        $scope.columns = columns;
        $scope.title = 'APPLICATIONS.RUNTIME.LOG.MODAL_TITLE';
        $scope.close = function () {
          $uibModalInstance.dismiss('close');
        };
      }];

      $scope.openSelectColumnsModal = function () {
        $uibModal.open({
          templateUrl: 'views/log/columns_modal.html',
          controller: ModalInstanceCtrl,
          resolve: {
            columns: function () {
              return $scope.columns;
            }
          }
        });
      };

      //////////////////////////////////
      // log type icons
      //////////////////////////////////
      $scope.typeToClass = {
        'a4c_workflow_event':   'fa fa-code-fork fa-rotate-90 fa-fw text-mute',
        'workflow_stage':       'fa fa-square-o fa-fw text-mute',
        'workflow_started':     'fa fa-play fa-fw text-primary',
        'workflow_succeeded':   'fa fa-check-square fa-fw text-success',
        'workflow_failed':      'fa fa-times fa-fw text-danger',
        'workflow_node_event':  'fa fa-square-o fa-fw text-mute',
        'sending_task':         'fa fa-paper-plane fa-fw text-mute',
        'task_started':         'fa fa-play-circle fa-fw text-primary',
        'task_succeeded':       'fa fa-check-circle fa-fw text-success',
        'task_failed':          'fa fa-times-circle fa-fw text-danger',
        'task_rescheduled':     'fa fa-refresh fa-fw text-warning',
        'other':                'fa fa-circle-o fa-fw text-mute',
        '':                     'fa fa-circle-o fa-fw text-mute'
      };

      //////////////////////////////////
      // log level icons
      //////////////////////////////////
      $scope.levelToClass = {
        'INFO': 'fa fa-info-circle fa-fw text-info',
        'WARN': 'fa fa-exclamation-triangle fa-fw text-warning',
        'DEBUG': 'fa fa-bug fa-fw text-mute',
        'ERROR': 'fa fa-times-circle fa-fw text-danger'
      };

      //////////////////////////////////
      // display panel size
      //////////////////////////////////
      function onResize(width, height) {
        // HACK: the minus 10 is a workaround for getting ride of the browser scrollbar that can appear sometime
        // probable root cause: the layout has changed after the onResize method has been called
        $scope.heightInfo = {height: height - 10};
        $scope.$digest();
      }
      resizeServices.registerContainer(onResize, '#log-search-result-container');

    }]);

});
