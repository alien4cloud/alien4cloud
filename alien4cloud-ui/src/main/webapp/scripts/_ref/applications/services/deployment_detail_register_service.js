// Simplify the generation of crud resources for alien
// Simplify the generation of crud resources for alien
define(function (require) {
  'use strict';
  var states = require('states');
  var _ = require('lodash');
  require('scripts/_ref/applications/controllers/deployment_history/deployment_history');
  require('scripts/_ref/applications/controllers/deployment_history/deployment_detail_info');
  require('scripts/_ref/applications/controllers/deployment_history/deployment_detail_tasks');

  return function(prefix, getSearchParam) {

    /*
    * list state
    */
    states.state(prefix+'.list', {
      url: '',
      templateUrl: 'views/_ref/applications/deployment_history/deployment_history_list.html',
      controller: 'DeploymentHistoryCtrl',
      resolve: {
        historyConf: ['$stateParams',
          function($stateParams) {
            return {
              rootState: prefix,
              searchParam: getSearchParam($stateParams)
            };
          }
        ]
      }
    });

    states.forward(prefix, prefix + '.list');

    /*
    *detail state
    */
    var detailState = prefix + '.detail';
    states.state(detailState, {
      url: '/:deploymentId',
      template: '<ui-view/>',
      resolve: {
        deploymentDTO: ['deploymentServices', '$stateParams',
          function(deploymentServices, $stateParams) {
            return _.defined($stateParams.deploymentDTO) ? {data: $stateParams.deploymentDTO} : _.catch(function() {
              return deploymentServices.getById({
                deploymentId: $stateParams.deploymentId
              }).$promise;
            });
          }
        ]
      },
      onEnter: ['breadcrumbsService','$filter','$state', 'deploymentDTO', function(breadcrumbsService, $filter, $state, deploymentDTO){
        breadcrumbsService.putConfig({
          state: detailState,
          text: function () {
            return $filter('date')(deploymentDTO.data.deployment.startDate, 'medium');
          }
        });
      }],
      params: {
        // optional deploymentDTO object related to this state. Will save a backend request
        deploymentDTO: null
      }
    });

    states.forward(detailState, detailState + '.info');

    /*
    * detail info state
    */
    var detailInfoState = detailState + '.info';
    states.state(detailInfoState, {
      url: '/info',
      templateUrl: 'views/_ref/applications/deployment_history/deployment_detail_info.html',
      controller: 'DeploymentDetailInfoCtrl',
      menu: {
        id: detailInfoState,
        state: detailInfoState,
        key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT_INFO',
        icon: 'fa fa-info',
        priority: 100
      }
    });

    // execution tasks state
    var detailInfoTaskListState = detailState + '.tasks';
    states.state(detailInfoTaskListState, {
      url: '/execution/:executionId',
      templateUrl: 'views/_ref/applications/deployment_history/deployment_detail_tasks.html',
      controller: 'DeploymentExecutionDetailInfoCtrl',
      onEnter: ['breadcrumbsService','$filter','$state', function(breadcrumbsService, $filter, $state) {
        breadcrumbsService.putConfig({
          state: detailInfoTaskListState,
          text: function () {
            return (_.defined($state.params.execution.workflowName)) ? $state.params.execution.workflowName : $state.params.execution.id;
          }
        });
      }],
      params: {
        execution: null,
        executionId: null
      }
    });

  };
});
