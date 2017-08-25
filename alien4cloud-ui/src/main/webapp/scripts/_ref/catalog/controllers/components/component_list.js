// components list: browse and search for components
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  require('scripts/common/services/resize_services');
  // make sure that required directives are loaded
  require('scripts/components/directives/search_node_type');

  // load other locations to manage components
  require('scripts/_ref/catalog/controllers/components/component_details');

  states.state('catalog.components.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/components/component_list.html',
    controller: 'SearchComponentCtrl2',
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
  });

  modules.get('a4c-components', ['ui.router', 'a4c-auth', 'a4c-common']).controller('SearchComponentCtrl2', ['$scope', '$state', 'resizeServices', 'authService', 'defaultFilters', 'badges',
    function ($scope, $state, resizeServices, authService, defaultFilters, badges) {
      $scope.isComponentManager = authService.hasRole('COMPONENTS_MANAGER');

      $scope.defaultFilters = defaultFilters;
      $scope.badges = badges;

      $scope.uploadSuccessCallback = function (data) {
        $scope.refresh = data;
      };

      $scope.openComponent = function (component) {
        $state.go('catalog.components.detail', {id: component.id});
      };

      function onResize(width, height) {
        $scope.heightInfo = {height: height};
        $scope.widthInfo = {width: width};
        $scope.$digest();
      }

      resizeServices.register(onResize, 0, 0);
    }
  ]);
});
