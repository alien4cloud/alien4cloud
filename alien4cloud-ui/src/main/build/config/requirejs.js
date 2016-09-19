// Require js optimizer configuration
module.exports = function (grunt) {
  var config = {
    options: {
      appDir: '<%= yeoman.dist %>',
      dir: '<%= yeoman.dist %>',
      mainConfigFile:'./src/main/webapp/scripts/require.config.js',
      modules:[
        {
          name:'a4c-bootstrap',
          exclude: [
            'lodash-base',
            'jquery',
            'angular',
            'angular-cookies',
            'angular-bootstrap',
            'angular-bootstrap-datetimepicker',
            'angular-resource',
            'angular-sanitize',
            'angular-ui-router',
            'angular-translate-base',
            'angular-translate',
            'angular-animate',
            'angular-xeditable',
            'angular-ui-select',
            'angular-tree-control',
            'autofill-event',
            'ng-table',
            'toaster',
            'hopscotch',
            'angular-file-upload',
            'angular-ui-ace',
            'angular-hotkeys',
            'ace',
            'sockjs',
            'stomp',
            'd3',
            'd3-tip',
            'd3-pie',
            'dagre',
            'graphlib'
          ]
        }
      ],

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
    dist: {
      options: {
        optimize: 'uglify',
        // optimize: 'none',
        optimizeCss: 'none',
        optimizeAllPluginResources: false,
        removeCombined: true,
      }
    },
    noOptimize: {
      options: {
        optimize: 'none',
        optimizeCss: 'none',
        optimizeAllPluginResources: false,
        removeCombined: false,
      }
    }
  };
  return config;
};
