/* global UTILS */

'use strict';

angular.module('alienUiApp').controller('ApplicationCtrl', ['$rootScope', '$scope', 'alienAuthService', 'application', 'applicationEventServices', '$state', 'environments',
  function($rootScope, $scope, alienAuthService, applicationResult, applicationEventServices, $state, environments) {

    var pageStateId = 'application.side.bar';
    $scope.application = applicationResult.data;
    var isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    var isDeployer = alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
    var isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    var isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');

    console.log('---- Application page loading');
    console.log('Application userRoles        : ', $scope.application.userRoles);
    console.log('Application groupRoles       : ', $scope.application.groupRoles);
    console.log('Application page IS MANAGER  : ', isManager);
    console.log('Application page IS DEPLOYER : ', isDeployer);
    console.log('Application page IS DEVOPS   : ', isDevops);
    console.log('Application page IS USER     : ', isUser);
    console.log('-----------------------------');

    // get all environments
    $scope.environments = environments;

    // select one environment by default
    // var selectDefaultEnvironment = function() {
    if (UTILS.isUndefinedOrNull($scope.selectedEnvironment)) {
      console.log('LOAD DEFAULT ENV', $scope.selectedEnvironment, UTILS.isUndefinedOrNull($scope.selectedEnvironment));
      $scope.environments.$promise.then(function getSelected(environments) {
        // Select initial default environment
        // TODO : select by most recent version ?
        console.log('Loading default env.....', environments.data);
        $scope.selectedEnvironment = environments.data.data[0];

        console.log('---- Environment page loading');
        console.log('Environment                  : ', $scope.selectedEnvironment);
        console.log('Environment page IS DEPLOYER : ', alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER'));
        console.log('Environment page IS USER     : ', alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER'));
        console.log('-----------------------------');

      });
    }
    // };
    // selectDefaultEnvironment();

    // switch environment for all application.detail child states
    $scope.changeEnvironment = function(switchToEnvironment) {
      var currentEnvironment = $scope.selectedEnvironment;
      var newEnvironment = switchToEnvironment;
      if (currentEnvironment.id != newEnvironment.id) {
        $scope.selectedEnvironment = switchToEnvironment;
      }
    };

    // start listening immediately if deployment active exists
    applicationEventServices.start();

    var setRuntimeDisabled = function() {
      for (var i = 0; i < $scope.menu.length; i++) {
        if ($scope.menu[i].id === 'am.applications.detail.runtime') {
          $scope.menu[i].disabled = UTILS.isUndefinedOrNull($scope.deploymentStatus) ||
            $scope.deploymentStatus === 'UNDEPLOYED' ||
            $scope.deploymentStatus === 'UNDEPLOYMENT_IN_PROGRESS' ||
            $scope.deploymentStatus === 'UNKNOWN';
        }
      }
    };

    applicationEventServices.refreshApplicationStatus(function(newStatus) {
      applicationEventServices.subscribeToStatusChange(pageStateId, function(type, event) {
        $scope.deploymentStatus = event.deploymentStatus;
        setRuntimeDisabled();
        $scope.$apply();
      });
      $scope.deploymentStatus = newStatus;
      setRuntimeDisabled();
    });

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

    //workaround
    //listen to 'DEPLOYMENT_IN_PROGRESS' event, to change the runtime button status
    $rootScope.$on('DEPLOYMENT_IN_PROGRESS', function() {
      $scope.deploymentStatus = 'DEPLOYMENT_IN_PROGRESS';
      setRuntimeDisabled();
    });

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
