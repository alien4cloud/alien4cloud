module.exports = function (grunt) {
  var tasks = ['clean:server', 'concurrent:test', 'autoprefixer', 'connect:test']; //, 'protractor_webdriver:start'];

  var registerLocal = function(name, launchTask) {
    var itTasks = tasks.slice(3); // copy and create a new array
    itTasks.push(launchTask);
    grunt.registerTask(name, itTasks);
  };

  var registerIt = function(name, launchTask) {
    var itTasks = tasks.slice(0); // copy and create a new array
    itTasks.push(launchTask);
    grunt.registerTask(name, itTasks);
  };

  var registerChromeIt = function(name, launchTask) {
    var chromeTasks = tasks.slice(0); // copy and create a new array
    chromeTasks.push(launchTask);
    grunt.registerTask(name, '', function() {
      var tasks = chromeTasks;
      grunt.option('force', true);
      grunt.task.run(tasks);
    });
  };

  registerChromeIt('chrome-ittest', 'protractor:runChrome');

  registerIt('firefox-ittest', 'protractor:runFirefox');
  registerIt('iexplore-ittest', 'protractor:runIexplore');
  registerLocal('local-ittest', 'protractor:runLocalserver');
  registerChromeIt('ittest-admin', 'protractor:runAdmin');
  registerChromeIt('ittest-application', 'protractor:runApplication');
  registerChromeIt('ittest-applicationTopology', 'protractor:runApplicationTopology');
  registerChromeIt('ittest-deployment', 'protractor:runDeployment');
  registerChromeIt('ittest-components', 'protractor:runComponents');
  registerChromeIt('ittest-commonAndSecurity', 'protractor:runCommonAndSecurity');
};
