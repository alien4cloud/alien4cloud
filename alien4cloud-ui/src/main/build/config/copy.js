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
        'bower_components/**/*',
        'views/alien4cloud-templates.js',
        'js-lib/**/*',
        'images/**/*',
        'scripts/**/*',
        'data/**/*',
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
  },
  bower: {
    files: [{
      expand: true,
      dot: true,
      cwd: '<%= yeoman.app %>',
      dest: '<%= yeoman.dist %>',
      src: [
        'bower_components/es5-shim/es5-shim.min.js',
        'bower_components/json3/lib/json3.min.js',
        'bower_components/requirejs/require.js',
        'bower_components/font-awesome/**/*',
        'bower_components/ace-builds/src-min-noconflict/**/*',
        'bower_components/bootstrap-sass-official/assets/fonts/bootstrap/**/*',
        'bower_components/roboto-fontface/**/*'
      ]
    }]
  }
};
