'use strict';

// Copies files not processed by requirejs optimization from source to dist so other tasks can process them
module.exports = {
  dist: {
    files: [{
      expand: true,
      dot: true,
      cwd: '<%= yeoman.app %>',
      dest: '<%= yeoman.dist %>',
      src: [
        '*.{ico,png,txt}',
        '.htaccess',
        '*.html',
        'views/**/*.html',
        'bower_components/**/*',
        'js-lib/**/*',
        'images/**/*',
        'data/**/*',
        'scripts/**/*',
        'api-doc/**/*',
        'version.json',
      ]
    }, {
      expand: true,
      cwd: '.tmp/images',
      dest: '<%= yeoman.dist %>/images',
      src: ['generated/*']
    }, {
      expand: true,
      flatten: true,
      cwd: '<%= yeoman.app %>',
      dest: '<%= yeoman.dist %>/images',
      src: ['bower_components/angular-tree-control/images/*']
    }]
  }
};