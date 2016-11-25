
define(function (require) {
  'use strict';

  var modules = require('modules');
  var angular = require('angular');

  modules.get('a4c-auth', ['a4c-search']).factory('quickSearchServices', ['$state','$resource',
    function($state, $resource ) {
      var quickSearchResource = $resource('rest/latest/quicksearch', {}, {
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

      function getComponentIcon(tags){
        for ( var i in tags) {
          var tag = tags[i];
          if(tag.name === 'icon'){
            return tag.value;
          }
        }
      }

      var quickSearch={};

      quickSearch.doQuickSearch= function(keyword) {
        var searchRequestObject = {
          'query': keyword,
          'from': 0,
          'size': 10
        };
        return quickSearchResource.search([], angular.toJson(searchRequestObject) ).$promise.then(function(result){
          var formatedData=result.data.data;
          for (var i = 0; i < formatedData.length; i++) {
            formatedData[i].type = result.data.types[i];
            if(formatedData[i].type ==='nodetype') {
              formatedData[i].icon = getComponentIcon(formatedData[i].tags);
            }
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
