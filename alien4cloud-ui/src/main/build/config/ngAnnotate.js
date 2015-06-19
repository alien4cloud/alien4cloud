'use strict';

// Allow the use of non-minsafe AngularJS files. Automatically makes it minsafe compatible so Uglify does not destroy the ng references
module.exports = {
  dist: {
    options: {
      singleQuotes: true,
    },
    dist: {
      files: [{
        expand: true,
        cwd: '<%= yeoman.app %>',
        src: 'scripts/**/*.js',
        dest: '<%= yeoman.dist %>'
      }]
    }
  }
};