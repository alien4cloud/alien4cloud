/* global UTILS */

'use strict';

angular.module('alienUiApp').controller('ApplicationCtrl', ['$rootScope', '$scope', 'alienAuthService', 'application', 'applicationEventServices', '$state', 'applicationEnvironmentServices', 'appEnvironments',
  function($rootScope, $scope, alienAuthService, applicationResult, applicationEventServices, $state, applicationEnvironmentServices, appEnvironments) {

    var pageStateId = 'application.side.bar';
    $scope.application = applicationResult.data;

    $scope.refreshAppStatus = function refreshAppStatus() {
      applicationEventServices.refreshApplicationStatus($scope.selectedEnvironment.id, function(newStatus) {
        applicationEventServices.subscribeToStatusChange(pageStateId, function(type, event) {
          $scope.deploymentStatus = event.deploymentStatus;
          setRuntimeDisabled();
          $scope.$apply();
        });
        $scope.deploymentStatus = newStatus;
        setRuntimeDisabled();
      });
    }

    // get environments
    $scope.envs = appEnvironments;
    // First selectedEnvironment set -> other pages will affect this value
    $scope.selectedEnvironment = $scope.selectedEnvironment || appEnvironments[0];

    // Application rights
    var isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    var isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');

    // Application environment rights
    var isDeployer = alienAuthService.hasResourceRole($scope.selectedEnvironment, 'DEPLOYMENT_MANAGER');
    var isUser = alienAuthService.hasResourceRole($scope.selectedEnvironment, 'APPLICATION_USER');

    console.log('---- Application page loading');
    console.log('Application userRoles        : ', $scope.application.userRoles);
    console.log('Application groupRoles       : ', $scope.application.groupRoles);
    console.log('Application page IS MANAGER  : ', isManager);
    console.log('Application page IS DEVOPS   : ', isDevops);
    console.log('-----------------------------');
    console.log('---- Environment page loading');
    console.log('Environment                  : ', $scope.selectedEnvironment);
    console.log('Environment page IS DEPLOYER : ', isDeployer);
    console.log('Environment page IS USER     : ', isUser);
    console.log('-----------------------------');

    // start listening immediately if deployment active exists
    applicationEventServices.start();

    var setRuntimeDisabled = function() {
      // get newest environments statuses
      var envs = applicationEnvironmentServices.getAllEnvironments($scope.application.id);
      envs.then(function updateRuntimeButton(result) {
        for (var i = 0; i < $scope.menu.length; i++) {
          if ($scope.menu[i].id === 'am.applications.detail.runtime') {
            $scope.menu[i].disabled = true;
            var countRunningEnv = 0;
            var newEnvs = result.data.data;
            for (var j = 0; j < newEnvs.length; j++) {
              if (newEnvs[j].status == 'DEPLOYED') {
                countRunningEnv++;
              }
            }
            $scope.menu[i].disabled = countRunningEnv == 0;
            return;
          }
        }
      });
    };

    // Stop listening if deployment active exists
    $scope.$on('$destroy', function() {
      applicationEventServices.stop();
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
      show: (isManager || isDeployer || isDevops || isUser)
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
    }, {
      id: 'am.applications.detail.runtime',
      state: 'applications.detail.runtime',
      key: 'NAVAPPLICATIONS.MENU_RUNTIME',
      icon: 'fa fa-cogs',
      show: (isManager || isDeployer)
    }, {
      id: 'am.applications.detail.users',
      state: 'applications.detail.users',
      key: 'NAVAPPLICATIONS.MENU_USERS',
      icon: 'fa fa-users',
      show: isManager
    }, {
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
    }];
  }
]);
