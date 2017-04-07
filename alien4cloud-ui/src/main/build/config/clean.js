'use strict';

// Empties folders to start fresh
module.exports = {
  dist: {
    files: [{
      dot: true,
      src: ['.tmp', '<%= yeoman.dist %>/*', '!<%= yeoman.dist %>/.git*']
    }]
  },
  server: '.tmp',
  // clean the bower components after requirejs task to avoid shipping useless files
  bower: '<%= yeoman.dist %>/bower_components/*'
};
