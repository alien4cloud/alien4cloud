// components list: browse and search for components
define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');


  states.state('catalog.archives.list', {
    url: '/list',
    templateUrl: 'views/_ref/catalog/archives/archives_list.html',
    controller: 'ArchivesListCtrl',
  });

  modules.get('a4c-components', ['ui.router', 'ui.bootstrap']).controller('ArchivesListCtrl', ['$scope',
    function ($scope) {

    }
  ]); // controller
});
