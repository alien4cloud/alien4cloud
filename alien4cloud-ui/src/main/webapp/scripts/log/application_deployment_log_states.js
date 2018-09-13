/* global define */

define(function(require) {
  'use strict';

  var states = require('states');
  var _ = require('lodash');

  //register runtime log state
  states.state('applications.detail.environment.deploycurrent.logs', {
    url: '/log',
    templateUrl: 'views/log/log_runtime.html',
    controller: 'LogController',
    //register breadcrumbs on entering the state
    onEnter: ['breadcrumbsService', '$translate', function(breadcrumbsService, $translate){
      breadcrumbsService.putConfig({
        state : 'applications.detail.environment.deploycurrent.logs',
        text: function(){
          return $translate.instant('APPLICATIONS.RUNTIME.LOG.MENU_ENTRY');
        }
      });
    }],
    menu: {
      id: 'applications.detail.environment.deploycurrent.logs',
      state: 'applications.detail.environment.deploycurrent.logs',
      key: 'APPLICATIONS.RUNTIME.LOG.MENU_ENTRY',
      icon: 'fa fa-newspaper-o',
      priority: 400
    },
    params: {
      instanceId: null,
      executionId: null,
      taskId: null
    }
  });


  // override deployment detail state so that we could ahve a menu-based ayout
  states.state('applications.detail.environment.history.detail', {
    url: '/:deploymentId',
    templateUrl: 'views/_ref/layout/vertical_menu_left_layout.html',
    controller: 'LayoutCtrl',
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
    //register breadcrumbs on entering the state
    onEnter: ['breadcrumbsService', '$filter', 'deploymentDTO', function(breadcrumbsService, $filter, deploymentDTO){
      breadcrumbsService.putConfig({
        state: 'applications.detail.environment.history.detail',
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

  states.state('applications.detail.environment.history.detail.logs', {
    url: '/log',
    templateUrl: 'views/log/log_deployment_history.html',
    controller: 'LogController',
    menu: {
      id: 'applications.detail.environment.history.detail.logs',
      state: 'applications.detail.environment.history.detail.logs',
      key: 'APPLICATIONS.RUNTIME.LOG.MENU_ENTRY',
      icon: 'fa fa-newspaper-o',
      priority: 400
    },
    params: {
      instanceId: null,
      executionId: null,
      taskId: null
    }
  });

});
