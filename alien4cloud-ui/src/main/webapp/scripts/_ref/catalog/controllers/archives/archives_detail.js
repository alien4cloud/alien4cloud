define(function (require) {
  'use strict';

  var modules = require('modules');
  var states = require('states');

  // register archives detail state
  states.state('catalog.archives.detail', {
    url: '/detail/:id',
    templateUrl: 'views/_ref/catalog/archives/archives_detail.html',
    controller: 'ArchivesDetailCtrl'
  });

  modules.get('a4c-components').controller(
    'ArchivesDetailCtrl', ['$scope',
    function($scope) {

    }
  ]); // controller
});
