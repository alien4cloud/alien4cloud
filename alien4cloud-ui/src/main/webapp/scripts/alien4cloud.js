define(function(require) {
  'use strict';

  var states = require('states');
  var modules = require('modules');
  var _ = require('lodash');
  var angular = require('angular');

  require('angular-cookies');
  require('angular-resource');
  require('angular-sanitize');
  require('angular-bootstrap');
  require('angular-ui-router');
  require('angular-translate');
  require('angular-xeditable');
  require('scripts/layout/layout');
  // require alien4cloud modules
  require('scripts/authentication/controllers/navbar');
  require('scripts/admin/admin');
  require('scripts/applications/controllers/application_list');
  require('scripts/users/controllers/users');
  require('scripts/clouds/controllers/cloud_list');
  require('scripts/components/controllers/component_list');
  require('scripts/topologytemplates/controllers/topology_template_list');

  var alien4cloud = modules.get('alien4cloud', [
    'ngCookies',
    'ngResource',
    'ngSanitize',
    'ui.bootstrap',
    'ui.router',
    'pascalprecht.translate',
    'xeditable'
  ]);

  // bootstrap angular js application
  states.state('home', {
    url: '/',
    templateUrl: 'views/main.html',
    controller: ['$scope', 'authService', 'hopscotchService', '$state', function($scope, authService, hopscotchService, $state) {
      $scope.isAdmin = false;
      var isAdmin = authService.hasRole('ADMIN');
      if(_.defined(isAdmin.then)) {
        authService.hasRole('ADMIN').then(function(result) {
          $scope.isAdmin = result;
        });
      } else {
        $scope.isAdmin = isAdmin;
      }
      $scope.adminTour = function() {
        $state.go('admin');
        hopscotchService.startTour('admin.home');
      };
    }]
  });
  states.state('restricted', {
    url: '/restricted',
    templateUrl: 'views/authentication/restricted.html'
  });

  require('scripts/common/services/rest_technical_error_interceptor');

  alien4cloud.startup = function() {
    // add requirements to alien4cloud
    modules.link(alien4cloud);

    alien4cloud.config(['$stateProvider', '$urlRouterProvider', '$httpProvider',
      function($stateProvider, $urlRouterProvider, $httpProvider) {
        $httpProvider.interceptors.push('restTechnicalErrorInterceptor');
        $urlRouterProvider.otherwise('/');
        states.config($stateProvider);
      }
    ]);

    // TODO load more modules
    alien4cloud.config(['$translateProvider',
      function($translateProvider) {
        $translateProvider.translations({CODE: 'fr-fr'});
        // Default language to load
        // $translateProvider.preferredLanguage('en-us');
        $translateProvider.preferredLanguage('fr-fr');

        // Static file loader
        $translateProvider.useStaticFilesLoader({
          prefix: 'data/languages/locale-',
          suffix: '.json'
        });
      }
    ]);

    alien4cloud.run(['$rootScope', '$state', 'editableOptions', 'editableThemes', 'authService',
      function($rootScope, $state, editableOptions, editableThemes, authService) {
        // check when the state is about to change
        $rootScope.$on('$stateChangeStart', function(event, toState) {
          authService.getStatus().$promise.then(function() {
            // check all the menu array & permissions
            authService.menu.forEach(function(menuItem) {
              var menuType = menuItem.id.split('.')[1];
              var foundMenuIndex = toState.name.indexOf(menuType);
              if (foundMenuIndex === 0 && menuItem.hasRole === false) {
                $state.go('restricted');
              }
            });
          });
        });

        // state routing
        $rootScope.$on('$stateChangeSuccess', function(event, toState) {
          var forward = states.stateForward[toState.name];
          if (_.defined(forward)) {
            $state.go(forward);
          }
        });

        /* angular-xeditable config */
        editableThemes.bs3.inputClass = 'input-sm';
        editableThemes.bs3.buttonsClass = 'btn-sm';
        editableThemes.bs3.submitTpl = '<button type="button" class="btn btn-primary"' +
          ' confirm="{{\'CONFIRM_MESSAGE\' | translate}}"' +
          ' confirm-title="{{\'CONFIRM\' | translate }}"' +
          ' confirm-placement="left"' +
          ' cancel-handler="$form.$cancel()"' +
          ' ng-click="$event.stopPropagation();">' +
          '<span class="fa fa-check"></span>' +
          '</button>';
        editableThemes.bs3.cancelTpl = '<button type="button" class="btn btn-default" ng-click="$form.$cancel()">' +
          '<span class="fa fa-times"></span>' +
          '</button>';
        editableOptions.theme = 'bs3';
      }
    ]);

    angular.bootstrap(document, ['alien4cloud']);
  };

  return alien4cloud;
});
