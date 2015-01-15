'use strict';

angular.module('alienUiApp').controller('ApplicationCtrl', ['$rootScope', '$scope', 'alienAuthService', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'environmentEventServicesFactory',
  function($rootScope, $scope, alienAuthService, applicationResult, $state, applicationEnvironmentServices, appEnvironments,
    environmentEventServicesFactory) {
    var environments = appEnvironments.environments;
    appEnvironments.deployEnvironments = [];
    var application = applicationResult.data;
    $scope.application = application;

    // Application rights
    var isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    var isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    // Application environment rights. Manager has right anyway, for other users we check all environments (see below)
    var isDeployer = isManager;

    var runtimeMenuItem = {
      id: 'am.applications.detail.runtime',
      state: 'applications.detail.runtime',
      key: 'NAVAPPLICATIONS.MENU_RUNTIME',
      icon: 'fa fa-cogs',
      show: (isManager || isDeployer)
    };
    var updateRuntimeDisabled = function() {
      // get newest environments statuses
      var disabled = true;
      for(var i=0; i < environments.length && disabled; i++) {
        if ( !(environments[i].status === 'UNDEPLOYED' || environments[i].status === 'UNKNOWN') ) {
          disabled = false;
        }
      }
      runtimeMenuItem.show = (isManager || isDeployer);
      runtimeMenuItem.disabled = disabled;
    };

    var callback = function (environment, event) {
      environment.status = event.deploymentStatus;
      updateRuntimeDisabled();
      // update the current scope and it's child scopes.
      $scope.$digest();
    };

    var eventRegistrations = [];
    // for every environement register for deployment status update for enrichment.
    for(var i=0; i<environments.length; i++) {
      var environment = environments[i];
      var registration = environmentEventServicesFactory(application.id, environment, callback);
      eventRegistrations.push(registration);
      var isEnvDeployer = alienAuthService.hasResourceRole(environment, 'DEPLOYMENT_MANAGER')
      if(isManager || isEnvDeployer) {
        appEnvironments.deployEnvironments.push(environment);
      }
      isDeployer = isDeployer || isEnvDeployer;
    }
    updateRuntimeDisabled();
    // get environments
    $scope.envs = environments;

    // Stop listening if deployment active exists
    $scope.$on('$destroy', function() {
      for(var i=0; i < eventRegistrations.length; i++) {
        eventRegistrations[i].close();
      }
    });

    $scope.onItemClick = function($event, menuItem) {
      if (menuItem.disabled) {
        $event.preventDefault();
        $event.stopPropagation();
      }
    };

    $scope.menu = [{
      id: 'am.applications.info',
      state: 'applications.detail.info',
      key: 'NAVAPPLICATIONS.MENU_INFO',
      icon: 'fa fa-info',
      show: (isManager || isDeployer || isDevops)
    }, {
      id: 'am.applications.detail.topology',
      state: 'applications.detail.topology',
      key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
      icon: 'fa fa-sitemap',
      show: (isManager || isDevops)
    }, {
      id: 'am.applications.detail.plans',
      state: 'applications.detail.plans',
      key: 'NAVAPPLICATIONS.MENU_PLAN',
      icon: 'fa fa-sitemap fa-rotate-270',
      show: (isManager || isDevops)
    }, {
      id: 'am.applications.detail.deployment',
      state: 'applications.detail.deployment',
      key: 'NAVAPPLICATIONS.MENU_DEPLOYMENT',
      icon: 'fa fa-cloud-upload',
      show: (isManager || isDeployer)
    }, runtimeMenuItem, {
      id: 'am.applications.detail.versions',
      state: 'applications.detail.versions',
      key: 'NAVAPPLICATIONS.MENU_VERSIONS',
      icon: 'fa fa-tasks',
      show: isManager
    }, {
      id: 'am.applications.detail.environments',
      state: 'applications.detail.environments',
      key: 'NAVAPPLICATIONS.MENU_ENVIRONMENT',
      icon: 'fa fa-share-alt',
      show: isManager
    }, {
      id: 'am.applications.detail.users',
      state: 'applications.detail.users',
      key: 'NAVAPPLICATIONS.MENU_USERS',
      icon: 'fa fa-users',
      show: isManager
    }];
  }
]);
