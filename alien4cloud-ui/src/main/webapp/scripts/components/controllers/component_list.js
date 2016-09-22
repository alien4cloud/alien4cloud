// components list is the entry point for browsing and managing components in a4c
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/common/services/resize_services');
  // make sure that required directives are loaded
  require('scripts/components/directives/search_node_type');
  require('scripts/common/directives/upload');

  // load other locations to manage components
  require('scripts/components/controllers/component_details');
  require('scripts/components/controllers/component_info');
  require('scripts/components/controllers/csar_list');
  require('scripts/components/controllers/csar_git');
  require('scripts/components/controllers/repository_list');

  // register components root state
  states.state('components', {
    url: '/components',
    templateUrl: 'views/layout/vertical_menu_layout.html',
    controller: 'LayoutCtrl',
    menu: {
      id: 'menu.components',
      state: 'components',
      key: 'NAVBAR.MENU_COMPONENTS',
      icon: 'fa fa-cubes',
      priority: 30,
      roles: ['COMPONENTS_MANAGER', 'COMPONENTS_BROWSER']
    }
  });
  states.state('components.list', {
    url: '/list',
    templateUrl: 'views/components/component_list.html',
    controller: 'SearchComponentCtrl',
    resolve: {
      defaultFilters: [function () {
        return {};
      }],
      // badges to display. objet with the following properties:
      //   name: the name of the badge
      //   tooltip: the message to display on the tooltip
      //   imgSrc: the image to display
      //   canDislay: a funtion to decide if the badge is displayabe for a component. takes as param the component and must return true or false.
      //   onClick: callback for the click on the displayed badge. takes as param: the component, the $state object
      badges: [function () {
        return [];
      }]
    },
    menu: {
      id: 'cm.components.list',
      state: 'components.list',
      key: 'NAVBAR.MENU_COMPONENTS',
      icon: 'fa fa-cubes',
      priority: 10
    }
  });
  states.forward('components', 'components.list');



  modules.get('a4c-components', ['ui.router', 'a4c-auth', 'a4c-common']).controller('SearchComponentCtrl', ['$scope', '$state', 'resizeServices', 'authService', 'defaultFilters', 'badges',
    function ($scope, $state, resizeServices, authService, defaultFilters, badges) {
      $scope.isComponentManager = authService.hasRole('COMPONENTS_MANAGER');

      $scope.defaultFilters = defaultFilters;
      $scope.badges = badges;

      $scope.uploadSuccessCallback = function (data) {
        $scope.refresh = data;
      };

      $scope.openComponent = function (component) {
        $state.go('components.detail', {id: component.id});
      };

      function onResize(width, height) {
        $scope.heightInfo = {height: height};
        $scope.widthInfo = {width: width};
        $scope.$digest();
      }

      // register for resize events
      window.onresize = function () {
        $scope.onResize();
      };

      resizeServices.register(onResize, 0, 0);
      $scope.heightInfo = {height: resizeServices.getHeight(0)};
      $scope.widthInfo = {width: resizeServices.getWidth(0)};
    }
  ]);
});
