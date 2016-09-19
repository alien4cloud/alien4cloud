module.exports = function(grunt) {
  'use strict';
  // Load grunt tasks automatically
  require('load-grunt-tasks')(grunt);

  // load npm modules for grunt
  grunt.loadNpmTasks('grunt-connect-proxy');
  grunt.loadNpmTasks('grunt-protractor-runner');
  grunt.loadNpmTasks('grunt-protractor-webdriver');
  grunt.loadNpmTasks('grunt-ng-annotate');
  grunt.loadNpmTasks('grunt-contrib-requirejs');
  grunt.loadNpmTasks('grunt-execute');

  // Time how long tasks take. Can help when optimizing build times
  require('time-grunt')(grunt);

  var config = {
    // Project settings
    yeoman: {
      setup: 'src/main/build',
      app: 'src/main/webapp',
      test: 'src/test/webapp',
      tmp: 'target/tmp',
      dist: 'target/webapp'
    }
  };
  grunt.config.merge(config);

  // Load configuration from multiple files
  require('load-grunt-config')(grunt, {
    configPath: __dirname + '/src/main/build/config',
    init: true,
    config: config
  });

  console.log('Grunt configuration ', grunt.config('clean'));

  // register tasks from definition files.
  grunt.task.loadTasks('src/main/build/tasks');

  // default task
  grunt.registerTask('default', ['newer:jshint', 'test', 'build']);
};
