'use strict';

angular.module('alienAuth').controller('alienNavBarCtrl', ['$rootScope', '$scope', 'alienAuthService', 'alienNavBarService', '$translate', 'hopscotchService', '$http',
  function($rootScope, $scope, alienAuthService, alienNavBarService, $translate, hopscotchService, $http) {

    $scope.signIn = function() {
      var data = 'username=' + $scope.login.username + '&password=' + $scope.login.password + '&submit=Login';
      alienAuthService.logIn(data, $scope);
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
      alienAuthService.logOut();
    };

    $scope.search = function() {};

    $scope.menu = alienNavBarService.menu;
    $scope.quickSearchHandler = alienNavBarService.quickSearchHandler;

    alienAuthService.getStatus();

    /* i18n */
    $scope.changeLanguage = function(langKey) {
      $translate.uses(langKey);
    };

    $scope.status = alienAuthService;

    $scope.startTour = function() {
      hopscotchService.startTour();
    };
  }
]);