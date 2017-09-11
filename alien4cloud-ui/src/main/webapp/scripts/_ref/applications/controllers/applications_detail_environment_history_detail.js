define(function(require) {
  'use strict';

  var states = require('states');
  var _ = require('lodash');


  require('scripts/_ref/applications/controllers/applications_detail_environment_history_detail_info');

  states.state('applications.detail.environment.history.detail', {
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

  states.forward('applications.detail.environment.history.detail','applications.detail.environment.history.detail.info');
});
