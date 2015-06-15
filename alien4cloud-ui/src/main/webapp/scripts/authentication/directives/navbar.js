'use strict';

angular.module('alienAuth').directive('alienNavBar',
  function() {

    return {
      templateUrl: 'views/authentication/navbar.html',
      restrict: 'E',
      link: function postLink() {
      }
    };
  });
