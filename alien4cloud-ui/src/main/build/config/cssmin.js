'use strict';

// Performs minification of css files.
module.exports = {
  dist:{
    files:{
      '<%= yeoman.dist %>/styles/main.css':[
        '.tmp/concat/styles/{,*/}*.css'
      ]
    }
  }
};
