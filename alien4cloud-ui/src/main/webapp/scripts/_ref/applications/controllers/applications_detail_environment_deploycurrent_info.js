define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var alienUtils = require('scripts/utils/alien_utils');
  require('scripts/deployment/directives/display_outputs');

  states.state('applications.detail.environment.deploycurrent.info', {
    url: '/info',
    templateUrl: 'views/_ref/applications/applications_detail_environment_deploycurrent_info.html',
    controller: 'ApplicationEnvDeployCurrentInfoCtrl',
    menu: {
      id: 'applications.detail.environment.deploycurrent.info',
      state: 'applications.detail.environment.deploycurrent.info',
      key: 'NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO',
      icon: 'fa fa-info',
      priority: 100
    }
  });

  modules.get('a4c-applications').controller('ApplicationEnvDeployCurrentInfoCtrl',
  ['$scope', 'applicationServices', 'application', '$state','breadcrumbsService', '$translate',
  function($scope, applicationServices, applicationResult, $state, breadcrumbsService, $translate) {

    breadcrumbsService.putConfig({
      state : 'applications.detail.environment.deploycurrent.info',
      text: function(){
        return $translate.instant('NAVAPPLICATIONS.MENU_DEPLOY_CURRENT.INFO');
      },
      onClick: function(){
        $state.go('applications.detail.environment.deploycurrent.info');
      }
    });

    $scope.applicationServices = applicationServices;
    $scope.fromStatusToCssClasses = alienUtils.getStatusIconCss;
    $scope.application = applicationResult.data;

    applicationServices.getActiveDeployment.get({
      applicationId: $scope.application.id,
      applicationEnvironmentId: $scope.environment.id
    }, undefined, function(success) {
      if (_.defined(success.data)) {
        $scope.activeDeployment = success.data;
        $scope.deployedTime = new Date() - $scope.activeDeployment.startDate;
      }
    });

    $scope.$on('a4cRuntimeTopologyLoaded', function() {
      $scope.locationResources = {};
      _.each(_.keys($scope.topology.topology.substitutedNodes), function (name) {
        $scope.locationResources[name] = $scope.topology.topology.nodeTemplates[name];
      });
    });

    // switch back to 'current deploy' when undeployed completed
    $scope.$watch('environment', function () {
      if ($scope.environment.status === 'UNDEPLOYED') {
        $state.go('applications.detail.environment.deploynext');
      }
    }, true);
  }
]);
});
