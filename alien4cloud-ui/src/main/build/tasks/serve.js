module.exports = function (grunt) {

  // build, then zip and upload to s3
  grunt.registerTask('serve', function(target) {
    if (target === 'dist') {
      return grunt.task.run(['build', 'connect:dist:keepalive']);
    }

    grunt.task.run(['clean:server', 'concurrent:server', 'autoprefixer', 'configureProxies:server', 'connect:livereload', 'watch']);
  });
};