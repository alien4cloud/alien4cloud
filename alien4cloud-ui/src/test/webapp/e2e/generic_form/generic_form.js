/* global by, expect, protractor, browser */
'use strict';

var common = require('../common/common');
var xedit = require('../common/xedit');

function sendValueToPrimitiveSelect(path, value) {
  common.select(by.id('primitiveTypeFormLabel' + path + 'input'), value);
}
module.exports.sendValueToPrimitiveSelect = sendValueToPrimitiveSelect;

function sendValueToPrimitiveRadio(path, value) {
  common.click(by.id('primitiveTypeFormLabel' + path + value));
}
module.exports.sendValueToPrimitiveRadio = sendValueToPrimitiveRadio;

function sendValueToPrimitiveInput(path, value) {
  var container = common.element(by.id('primitiveTypeFormLabel' + path + 'input'));
  var inputElement = common.element(by.tagName('input'), container);
  inputElement.clear();
  inputElement.sendKeys(value);
}
module.exports.sendValueToPrimitiveInput = sendValueToPrimitiveInput;

function sendValueToPrimitive (path, value, withAutoCompletion, elementType) {
  if (elementType === 'tosca') {
    xedit.sendKeys('p_' + path, value);
  } else if (elementType === 'xeditable') {
    xedit.sendKeys('primitiveTypeFormLabel' + path + 'input', value, withAutoCompletion);
  } else if (elementType === 'select') {
    sendValueToPrimitiveSelect(path, value);
  } else if (elementType === 'radio') {
    sendValueToPrimitiveRadio(path, value);
  } else if (elementType === 'input') {
    sendValueToPrimitiveInput(path, value);
  } else {
    // Do we have an assert fail in protractor
    expect(false).toBe(true);
  }
}
module.exports.sendValueToPrimitive = sendValueToPrimitive;

var addElementsToArray = function(path, values, withAutoCompletion, elementTypes) {
  for (var i = 0; i < values.length; i++) {
    var value = values[i];
    common.click(by.id('treeForm' + path + 'label'));
    common.click(by.id('arrayTypeForm' + path + 'addelementbutton'));
    if (typeof value === 'string') {
      sendValueToPrimitive(path + i, value, withAutoCompletion, elementTypes);
    } else {
      common.click(by.id('complexTypeFormLabel' + path + i + 'editbutton'));
      for (var valueField in value) {
        if (value.hasOwnProperty(valueField)) {
          var autoComplete = withAutoCompletion.hasOwnProperty(valueField) ? withAutoCompletion[valueField] : false;
          sendValueToPrimitive(path + i + valueField, value[valueField], autoComplete, elementTypes[valueField]);
        }
      }
    }
  }
};
module.exports.addElementsToArray = addElementsToArray;

var addKeyToMap = function(path, keyValue, value, withAutoCompletion, elementType) {
  common.click(by.id('treeForm' + path + 'label'));
  var keyInput = common.element(by.id('mapTypeForm' + path + 'keyinput'));
  keyInput.clear();
  keyInput.sendKeys(keyValue);
  common.click(by.id('mapTypeForm' + path + 'addelementbutton'));
  if (typeof value === 'string') {
    // Attention for the moment this case is not tested
  } else {
    common.click(by.id('complexTypeFormLabel' + path + keyValue + 'editbutton'));
    for (var valueField in value) {
      if (value.hasOwnProperty(valueField)) {
        var autoComplete = withAutoCompletion.hasOwnProperty(valueField) ? withAutoCompletion[valueField] : false;
        sendValueToPrimitive(path + keyValue + valueField, value[valueField], autoComplete, elementType[valueField]);
      }
    }
  }
};
module.exports.addKeyToMap = addKeyToMap;

function expectValueFromSelectPrimitive(path, value) {
  var selectElement = common.element(by.id('primitiveTypeFormLabel' + path + 'input'));
  expect(selectElement.getText()).toContain(value);
}
module.exports.expectValueFromSelectPrimitive = expectValueFromSelectPrimitive;

function expectValueFromRadioPrimitive(path, value) {
  var radioElement = common.element(by.id('primitiveTypeFormLabel' + path + value));
  expect(radioElement.getAttribute('class')).toMatch('active');
}
module.exports.expectValueFromSelectPrimitive = expectValueFromSelectPrimitive;

function expectValueFromPrimitive(path, value, elementType) {
  if (elementType === 'tosca') {
    xedit.expect('p_' + path, value);
  } else if (elementType === 'xeditable') {
    xedit.expect('primitiveTypeFormLabel' + path + 'input', value);
  } else if (elementType === 'select') {
    expectValueFromSelectPrimitive(path, value);
  } else if (elementType === 'radio') {
    expectValueFromRadioPrimitive(path, value);
  } else {
    // Do we have an assert fail in protractor
    expect(false).toBe(true);
  }
}
module.exports.expectValueFromPrimitive = expectValueFromPrimitive;

/*
 Verify that action has been well taken into account
 */
function expectValueFromMap(path, keyValue, value, elementTypes) {
  common.click(by.id('treeForm' + path + 'label'));
  if (typeof value === 'string') {
    // Attention for the moment this case is not tested
  } else {
    common.click(by.id('complexTypeFormLabel' + path + keyValue + 'editbutton'));
    for (var valueField in value) {
      if (value.hasOwnProperty(valueField)) {
        expectValueFromPrimitive(path + keyValue + valueField, value[valueField], elementTypes[valueField]);
      }
    }
  }
}
module.exports.expectValueFromMap = expectValueFromMap;

function expectValueFromArray(path, values, elementType) {
  for (var i = 0; i < values.length; i++) {
    var value = values[i];
    common.click(by.id('treeForm' + path + 'label'));
    if (typeof value === 'string') {
      expectValueFromPrimitive(path + i, value, elementType);
    } else {
      // Attention for the moment this case is not tested
    }
  }
}
module.exports.expectValueFromArray = expectValueFromArray;

function abortInput(path) {
  var input = common.element(by.id('primitiveTypeFormLabel' + path + 'input')).element(by.tagName('input'));
  input.sendKeys(protractor.Key.ESCAPE);
  browser.waitForAngular();
}
module.exports.abortInput = abortInput;

function expectConstraintAlertPresent(path) {
  var warnElement = common.element(by.id('primitiveTypeFormLabel' + path + 'input')).element(by.css('.text-danger'));
  expect(warnElement.isPresent()).toBe(true);
}
module.exports.expectConstraintAlertPresent = expectConstraintAlertPresent;

function saveForm() {
  common.click(by.binding('GENERIC_FORM.SAVE'));
  common.dismissAlertIfPresent();
}
module.exports.saveForm = saveForm;

function cancelForm() {
  common.click(by.binding('GENERIC_FORM.CANCEL'));
  common.dismissAlertIfPresent();
}
module.exports.cancelForm = cancelForm;
