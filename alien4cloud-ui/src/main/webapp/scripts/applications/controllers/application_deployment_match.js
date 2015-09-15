define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');
  var _ = require('lodash');
  var angular = require('angular');

  states.state('applications.detail.deployment.match', {
    url: '/match',
    templateUrl: 'views/applications/application_deployment_match.html',
    controller: 'ApplicationDeploymentMatchCtrl',
    menu: {
      id: 'am.applications.detail.deployment.match',
      state: 'applications.detail.deployment.match',
      key: 'APPLICATIONS.DEPLOYMENT.MATCHING',
//      icon: 'fa fa-cloud-upload',
      roles: ['APPLICATION_MANAGER', 'APPLICATION_DEPLOYER'], // is deployer
//      disabled:'!deploymentContext.selectedLocation',
      priority: 200
    }
  });

  modules.get('a4c-applications').controller('ApplicationDeploymentMatchCtrl',
    ['$scope', 'authService', '$upload', 'applicationServices', 'toscaService', '$resource', '$http', '$translate', 'application', '$state', 'applicationEnvironmentServices', 'appEnvironments', 'toaster', '$filter', 'menu',
    function($scope, authService, $upload, applicationServices, toscaService, $resource, $http, $translate, applicationResult, $state, applicationEnvironmentServices, appEnvironments, toaster, $filter, menu) {
    }
  ]); //controller
}); //Define
