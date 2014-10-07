'use strict';

angular.module('alienAuth').factory('alienNavBarService',
  function() {
    var menu = {
      'navbrandimg': '/images/brandsmall.png',
      'left': [
        {
          'key': 'HOME',
          'href': '#',
          'roles': []
        }
      ],
      'right': []
    };

    var navbarService = {
      'menu': menu,
      'quickSearchHandler': null
    };
    return navbarService;
  }
);
