define(function (require) {
  'use strict';

  var modules = require('modules');

  require('scripts/authentication/services/quicksearch');
  require('scripts/common/services/hopscotch_service');
  require('angular-translate');
  require('angular-bootstrap');
  require('angular-cookies');

  require('scripts/authentication/directives/navbar');
  require('scripts/authentication/services/authservices');


  modules.get('a4c-auth', ['pascalprecht.translate', 'ng-hopscotch']).controller('alienNavBarCtrl',
    ['$rootScope', '$scope', 'authService', 'quickSearchServices', '$translate', 'hopscotchService', '$http',
    function($rootScope, $scope, authService, quickSearchServices, $translate, hopscotchService, $http) {
      $scope.isCollapsed = true;
      $scope.login = {};
      $scope.signIn = function() {
        var data = 'username=' + $scope.login.username + '&password=' + $scope.login.password + '&submit=Login';
        authService.logIn(data, $scope);
      };

      // Recover alien version and display github tag link if it's not snapshot (cached request)
      $http.get('/version.json', {
        cache: 'true'
      }).then(function(result) {
        $scope.version = {};
        $scope.version.tag = result.data.version;
        $scope.version.isSnapshot = $scope.version.tag.indexOf('SNAPSHOT') > -1;
      });

      // Basic Spring security logout
      $scope.logout = function() {
        authService.logOut();
      };

      $scope.search = function() {};

      $scope.menu = authService.menu;
      $scope.quickSearchHandler = {
        'doQuickSearch': quickSearchServices.doQuickSearch,
        'onItemSelected': quickSearchServices.onItemSelected,
        'waitBeforeRequest': 500,
        'minLength': 3
      };

      authService.getStatus();

      $http.get('/rest/latest/configuration/supportedLanguages').then(function(result) {
        $scope.supportedLanguages = result.data.data;
      });

      /* i18n */
      $scope.changeLanguage = function(langKey) {
        $translate.use(langKey);
      };

      $scope.status = authService;
      $scope.internalLogin = false;
      $scope.displayInternalLogin = function() {
        $scope.internalLogin=true;
      };

      $scope.startTour = function() {
        hopscotchService.startTour();
      };
    }
  ]);
});
