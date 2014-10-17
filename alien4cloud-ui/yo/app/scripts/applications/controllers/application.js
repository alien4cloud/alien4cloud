/* global UTILS */

'use strict';

angular.module('alienUiApp').controller('ApplicationCtrl', ['$rootScope', '$scope', 'alienAuthService', 'application', 'applicationEventServices', '$state',
  function($rootScope, $scope, alienAuthService, applicationResult, applicationEventServices, $state) {
    $scope.application = applicationResult.data;
    var isManager = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_MANAGER');
    var isDeployer = alienAuthService.hasResourceRole($scope.application, 'DEPLOYMENT_MANAGER');
    var isDevops = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_DEVOPS');
    var isUser = alienAuthService.hasResourceRole($scope.application, 'APPLICATION_USER');

    // Start listening immediately if deployment active exists
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
      applicationEventServices.subscribeToStatusChange($state.current.name, function(type, event) {
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
    }
    ;

    $scope.menu = [
      {
        id: 'am.applications.info',
        state: 'applications.detail.info',
        key: 'NAVAPPLICATIONS.MENU_INFO',
        icon: 'fa fa-info',
        show: (isManager || isDeployer || isDevops || isUser)
      },
      {
        id: 'am.applications.detail.topology',
        state: 'applications.detail.topology',
        key: 'NAVAPPLICATIONS.MENU_TOPOLOGY',
        icon: 'fa fa-sitemap',
        show: (isManager || isDevops)
      },
      {
        id: 'am.applications.detail.plans',
        state: 'applications.detail.plans',
        key: 'NAVAPPLICATIONS.MENU_PLAN',
        icon: 'fa fa-sitemap fa-rotate-270',
        show: (isManager || isDevops)
      },
      {
        id: 'am.applications.detail.deployment',
        state: 'applications.detail.deployment',
        key: 'NAVAPPLICATIONS.MENU_DEPLOYMENT',
        icon: 'fa fa-cloud-upload',
        show: (isManager || isDeployer)
      },
      {
        id: 'am.applications.detail.runtime',
        state: 'applications.detail.runtime',
        key: 'NAVAPPLICATIONS.MENU_RUNTIME',
        icon: 'fa fa-cogs',
        show: (isManager || isDeployer)
      },
      {
        id: 'am.applications.detail.users',
        state: 'applications.detail.users',
        key: 'NAVAPPLICATIONS.MENU_USERS',
        icon: 'fa fa-users',
        show: isManager
      }
    ];
  }
])
;
