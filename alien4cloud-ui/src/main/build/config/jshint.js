'use strict';

// Make sure code styles are up to par and there are no obvious mistakes
module.exports = {
  options: {
    jshintrc: 'src/.jshintrc',
    reporter: require('jshint-stylish'),
    force: true
  },
  all: ['Gruntfile.js', '<%= yeoman.app %>/scripts/**/*.js'],
  test: {
    options: {
      jshintrc: 'src/.jshintrc'
    },
    src: ['<%= yeoman.test %>/spec/{,*/}*.js']
  }
};
