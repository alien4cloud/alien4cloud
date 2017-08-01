// Renames files for browser caching purposes
module.exports = {
  appFiles: {
    files: {
      src: [
        '<%= yeoman.dist %>/scripts/alien4cloud-dependencies.js',
        '<%= yeoman.dist %>/scripts/alien4cloud-bootstrap.js',
        '<%= yeoman.dist %>/styles/{,*/}*.css',
        '<%= yeoman.dist %>/views/{,*/}*.js',
        '<%= yeoman.dist %>/data/languages/{,*/}*.json'
      ]
    }
  },
  requireConfig: {
    files: {
      src: [
        '<%= yeoman.dist %>/scripts/require.config.js'
      ]
    }
  }
};
