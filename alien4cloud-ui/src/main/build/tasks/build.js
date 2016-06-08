module.exports = function (grunt) {
  grunt.registerTask('build',
    [
      'clean:dist', // cleanup dist folder
      'jshint:all',
      'concurrent:dist', // process compass and images
      'autoprefixer', // update CSS to add browser specifics (moz- etc.)
      'copy:dist', // copy resource files to dist folders
      'cssmin', // minify css
      'execute:prerequire',
      'requirejs:dist',
      //'rev', // rename files for caching purpose
      'htmlmin'
    ]
  );
};
