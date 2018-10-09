// Require js optimizer configuration
module.exports = function (grunt) {
  'use strict';
  var config = {
    options: {
      appDir: '<%= yeoman.dist %>',
      dir: '<%= yeoman.dist %>',
      mainConfigFile:'./src/main/webapp/scripts/require.config.js',
      baseUrl: '.',

      keepBuildDir: true,
      allowSourceOverwrites: true,

      fileExclusionRegExp: /^(bower_components|api-doc|data|images|js-lib|META-INF|styles|views)$/,
      findNestedDependencies: true,
      normalizeDirDefines: 'all',
      inlineText: true,
      skipPragmas: true,

      done: function (done, output) {
        var analysis = require('rjs-build-analysis');
        var tree = analysis.parse(output);
        var duplicates = analysis.duplicates(tree);

        if (duplicates.length > 0) {
          grunt.log.subhead('Duplicates found in requirejs build:');
          grunt.log.warn(duplicates);
          return done(new Error('r.js built duplicate modules, please check the excludes option.'));
        } else {
          var relative = [];
          var bundles = tree.bundles || [];
          bundles.forEach(function (bundle) {
            bundle.children.forEach(function (child) {
              if (child.match(/\.\//)) {
                relative.push(child + ' is relative to ' + bundle.parent);
              }
            });
          });

          if (relative.length) {
            grunt.log.subhead('Relative modules found in requirejs build:');
            grunt.log.warn(relative);
            return done(new Error('r.js build contains relative modules, duplicates probably exist'));
          }
        }
        done();
      }
    },
    a4cdist: {
      options: {
        // optimize: 'uglify',
        optimize: 'none',
        optimizeCss: 'none',
        optimizeAllPluginResources: false,
        removeCombined: true,
        modules:[
          {
            name:'a4c-bootstrap'
          }
        ],
        paths: {
          'a4c-templates': 'empty:',
          'a4c-dependencies': 'empty:',
          'lodash-base': 'empty:',
          'jquery': 'empty:',
          'angular': 'empty:',
          'angular-cookies': 'empty:',
          'angular-bootstrap': 'empty:',
          'angular-bootstrap-datetimepicker': 'empty:',
          'angular-bootstrap-datetimepicker-template': 'empty:',
          'moment': 'empty:',
          'angular-resource': 'empty:',
          'angular-sanitize': 'empty:',
          'angular-ui-router': 'empty:',
          'angular-translate-base': 'empty:',
          'angular-translate': 'empty:',
          'angular-translate-storage-cookie': 'empty:',
          'angular-animate': 'empty:',
          'angular-xeditable': 'empty:',
          'angular-ui-select': 'empty:',
          'angular-tree-control': 'empty:',
          'ng-table': 'empty:',
          'autofill-event': 'empty:',
          'toaster': 'empty:',
          'hopscotch': 'empty:',
          'angular-file-upload-shim': 'empty:',
          'angular-file-upload': 'empty:',
          'angular-ui-ace': 'empty:',
          'angular-hotkeys': 'empty:',
          'ace': 'empty:',
          'sockjs': 'empty:',
          'stomp': 'empty:',
          'clipboard': 'empty:',
          'd3': 'empty:',
          'd3-tip': 'empty:',
          'd3-pie': 'empty:',
          'dagre': 'empty:',
          'graphlib': 'empty:',
        }
      }
    },
    a4cdepdist: {
      options: {
        optimize: 'none',
        optimizeCss: 'none',
        optimizeAllPluginResources: false,
        removeCombined: false,
        modules:[
          {
            name:'a4c-dependencies',
          }
        ]
      }
    },
    noOptimize: {
      options: {
        optimize: 'none',
        optimizeCss: 'none',
        optimizeAllPluginResources: false,
        removeCombined: false
      }
    }
  };
  return config;
};
