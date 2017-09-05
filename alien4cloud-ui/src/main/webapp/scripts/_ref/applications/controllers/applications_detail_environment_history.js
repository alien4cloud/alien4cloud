define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');

  states.state('applications.detail.environment.history', {
    url: '/history',
    templateUrl: 'views/_ref/applications/applications_detail_environment_history.html',
    controller: 'ApplicationEnvHistoryCtrl',
    menu: {
      id: 'applications.detail.environment.history',
      state: 'applications.detail.environment.history',
      key: 'NAVAPPLICATIONS.MENU_HISTORY',
      icon: '',
      priority: 300
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvHistoryCtrl',
    ['$scope', '$translate', 'deploymentServices', 'breadcrumbsService',
      function ($scope, $translate, deploymentServices, breadcrumbsService) {

        $scope.now = new Date();
        
        breadcrumbsService.putConfig({
          state: 'applications.detail.environment.history',
          text: function () {
            return $translate.instant('NAVAPPLICATIONS.MENU_HISTORY');
          }
        });

        function processDeployments(deployments) {
          if (_.defined(deployments)) {
            _.each(deployments, function (deployment) {
              if(_.defined(deployment.deployment.endDate)){
                deployment.deployment.duration = '';
              }else{
                deployment.deployment.duration = '';
              }
            });
          }
        }

        deploymentServices.get({
          applicationId: $scope.application.id,
          includeSourceSummary: false
        }, function (result) {
          processDeployments(result.data);
          $scope.deployments = result.data;
        });
      }
    ]);
});
