define(function (require) {
  'use strict';

  var modules = require('modules');
  var _ = require('lodash');

  modules.get('a4c-common').factory('explorerService', [
    function() {
      return {
        // Map of file extensions to ace ide editor mode.
        extToMode: {
          yaml: 'yaml',
          yml:  'yaml',
          groovy: 'groovy',
          sh: 'sh',
          properties: 'properties',
          bat: 'batchfile',
          cmd: 'batchfile',
          py: 'python',
          defaultMode: 'yaml' // jshint ignore:line
        },
        getDefaultMode: function() {
          return this.extToMode.defaultMode;
        },
        getMode: function(node) {
          var fileName = node.fullPath;
          var tokens = fileName.trim().split('/');
          if (tokens.length > 0) {
            fileName = tokens[tokens.length - 1];
          }
          var fileExt = fileName.substr((fileName.lastIndexOf('.') -1 >>> 0) + 2).toLowerCase();
          return _.defined(this.extToMode[fileExt]) ? this.extToMode[fileExt] :this.extToMode.defaultMode; // jshint ignore:line
        },
        /**
        * Process a tree of node to expand all nodes that compose a given path.
        *
        * @param expandedNodes The list of nodes that are expanded.
        * @param fullPath The path for which to expand nodes.
        * @param node The current node under expansion
        * @param isExpandable A flag to know if we can expand the current node.
        */
        expand: function(expandedNodes, fullPath, node, isExpandable) {
          if(isExpandable) {
            if(fullPath !== node.fullPath && fullPath.indexOf(node.fullPath) > -1) {
              expandedNodes.push(node);
            }
          }
          if(_.defined(node) && _.defined(node.children)) {
            for(var i=0;i<node.children.length;i++) {
              this.expand(expandedNodes, fullPath, node.children[i], true);
              if(_.includes(expandedNodes, node.children[i])){
                break;
              }
            }
          }
        },
        /**
        * Get options for the treecontrol
        *
        * @param dirSelectable true if we can select a directory, false if not.
        */
        getOps: function(dirSelectable) {
          return {
            dirSelectable: dirSelectable,
            injectClasses: {
              iExpanded: 'fa',
              iCollapsed: 'fa',
              iLeaf: 'fa'
            },
            equality: function(node1, node2) {
              if(node1 && node2) {
                return node1.fullPath === node2.fullPath;
              }
              return false;
            }
          };
        }
        // end
      };
    }
  ]);
});
