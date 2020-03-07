// operations on topology archive browsing.
define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-topology-editor', ['ngResource']).factory('topoBrowserService', ['$resource', '$http',
    function($resource, $http) {

      var getContent = function(topology, node, callback, imageCallback){
        var selectedUrl;
        if(_.defined(node.artifactId)) {
          // temp file under edition
          selectedUrl = 'rest/latest/editor/' + topology.id + '/file/' + node.artifactId;
        } else {
          // commited file
          selectedUrl = 'static/tosca/' + topology.archiveName + '/' + topology.archiveVersion + node.fullPath;
        }
        _.isImage(selectedUrl).then(function(isImage) {
          if(isImage) {
            if(_.isFunction(imageCallback)) {
              imageCallback(selectedUrl);
            }
          } else {
            $http({method: 'GET',
              transformResponse: function(d) { return d; },
              url: selectedUrl})
              .then(callback);
          }
        });
      };

      return {
        getContent: getContent
      };
    }
  ]);
}); // define
