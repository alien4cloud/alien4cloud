'use strict';

var debugEnabled = false;
var fs = require('fs');
var path = require('path');
var screenShotBaseDir = '../target/screenshot'; // close to yo dir
var fileCounter = 0;
var SCREENSHOT = true; // or false to disable screenshot

//
// Take PNG screenshot at any time in tests and save it here in ${screenShotBaseDir}
// In you don't remove the ${screenShotBaseDir}, you may have screenshot names with counter (same old  name)
//

function writeScreenShot(data, filename) {
  var stream = fs.createWriteStream(filename);
  stream.write(new Buffer(data, 'base64'));
  stream.end();
}

var takeScreenShot = function(imgName) {
  if (SCREENSHOT === true) {
    var specDescription = jasmine.getEnv().currentSpec.description;
    if (debugEnabled) {
      console.log('Screenshot [' + imgName + '] in : ' + specDescription);
    }
    var dirExists = fs.existsSync(screenShotBaseDir);
    // create only one time the ${screenShotBaseDir} folder
    if (!dirExists) {
      fs.mkdirSync(screenShotBaseDir);
    }
    browser.takeScreenshot().then(function(png) {
      var imagePath = path.join(screenShotBaseDir, imgName + '.png');
      // if screenshot already exist : put a counter in the name
      if (fs.existsSync(imagePath)) {
        imagePath = path.join(screenShotBaseDir, imgName + '-' + fileCounter + '.png');
        fileCounter++;
      }
      writeScreenShot(png, imagePath);
    });
  }
};

module.exports.takeScreenShot = takeScreenShot;
