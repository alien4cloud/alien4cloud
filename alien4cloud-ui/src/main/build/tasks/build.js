module.exports = function (grunt) {
  grunt.registerTask('build',
    [
      'clean:dist', // cleanup dist folder
      'jshint:all',
      'concurrent:dist', // process compass and images
      'autoprefixer', // update CSS to add browser specifics (moz- etc.)
      'copy:dist', // copy resource files to dist folders
      // 'ngAnnotate:dist', // automatically update angular dependency injections to be ready for minification
      // 'cdnify', //
      'cssmin', // minify css
      'requirejs:dist',
      //'useminPrepare',
      //'rev', // rename files for caching purpose
      //'usemin',
      'htmlmin'
    ]
  );
};
