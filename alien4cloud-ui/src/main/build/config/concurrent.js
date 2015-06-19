'use strict';

// Run some tasks in parallel to speed up the build process
module.exports = {
  server: ['compass:server'],
  test: ['compass'],
  dist: [
    'compass:dist',
    // 'imagemin',
    'svgmin'
  ]
};