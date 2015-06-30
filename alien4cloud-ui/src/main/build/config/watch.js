'use strict';

module.exports = {
  js: {
    files: ['<%= yeoman.app %>/scripts/**/*.js'],
    // tasks: ['newer:jshint:all'],
    options: {
      livereload: true
    }
  },
  compass: {
    files: ['<%= yeoman.app %>/styles/{,*/}*.{scss,sass}'],
    tasks: ['compass:server', 'autoprefixer']
  },
  gruntfile: {
    files: ['Gruntfile.js']
  },
  livereload: {
    options: {
      livereload: '<%= connect.options.livereload %>'
    },
    files: ['<%= yeoman.app %>/views/**/*.html','.tmp/styles/{,*/}*.css']
  }
};
