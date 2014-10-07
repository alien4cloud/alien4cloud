'use strict';

angular.module('alienAuth').controller('alienNavBarCtrl', ['$rootScope', '$scope', 'alienAuthService', 'alienNavBarService', '$translate', function(
  $rootScope, $scope, alienAuthService, alienNavBarService, $translate) {
  $scope.signIn = function() {
    var data = 'username='+$scope.login.username+'&password='+$scope.login.password+'&submit=Login';
    alienAuthService.logIn(data, $scope);
  };

  // Basic Spring security logout
  $scope.logout = function(){
    alienAuthService.logOut();
  };

  $scope.search = function(){};

  $scope.menu = alienNavBarService.menu;
  $scope.quickSearchHandler = alienNavBarService.quickSearchHandler;

  alienAuthService.getStatus();

  /* i18n */
  $scope.changeLanguage = function (langKey) {
    $translate.uses(langKey);
  };

  $scope.status = alienAuthService;
}]);
