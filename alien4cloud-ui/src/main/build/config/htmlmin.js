'use strict';

// produce minified files in the dist folder
module.exports = {
  dist: {
    options: {
      collapseWhitespace: true,
      collapseBooleanAttributes: false,
      removeCommentsFromCDATA: true,
      removeOptionalTags: false
    },
    files: [{
      expand: true,
      cwd: '<%= yeoman.dist %>',
      // src: ['*.html', 'views/{,*/}*.html'],
      src: ['*.html', 'views/**/*.html'],
      dest: '<%= yeoman.dist %>'
    }]
  }
};