
define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');

  modules.get('a4c-tosca').factory('relationshipTypeQuickSearchService', ['$state','$resource',
    function($state, $resource ) {
      var quickSearchResource = $resource('rest/latest/quicksearch/relationship_types', {}, {
        'search': {
          method : 'POST',
          isArray : false,
          headers : {
            'Content-Type' : 'application/json; charset=UTF-8'
          }
        }
      });

      var openItem = {};
      openItem.nodetype = function(componentId){
        $state.go('components.detail', { id: componentId });
      };
      openItem.application = function(applicationId){
        $state.go('applications.detail.info', { id: applicationId });
      };

      var quickSearch={};

      quickSearch.doQuickSearch= function(keyword) {
        var searchRequestObject = {
          'query': keyword,
          'from': 0,
          'size': 10
        };
        return quickSearchResource.search([], angular.toJson(searchRequestObject) ).$promise.then(function(result){
          var formatedData= [];
          for (var i = 0; i < result.data.data.length; i++) {
            formatedData[i] = result.data.data[i].id;
          }
          return formatedData;
        });
      };

      quickSearch.onItemSelected = function(item) {
        openItem[item.type](item.id);
      };

      return quickSearch;
    }
  ]); // factory
}); // define
