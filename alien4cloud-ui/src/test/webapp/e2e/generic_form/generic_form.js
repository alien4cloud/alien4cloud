/* global element, by */
'use strict';

var common = require('../common/common');

/*
 Actions on XEditable
 */

var addElementsToArray = function(path, values, withAutoCompletion, elementTypes) {
  for (var i = 0; i < values.length; i++) {
    var value = values[i];
    var treeLabel = element(by.id('treeForm' + path + 'label'));
    treeLabel.click();
    var addElementButton = element(by.id('arrayTypeForm' + path + 'addelementbutton'));
    addElementButton.click();
    if (typeof value === 'string') {
      sendValueToPrimitive(path + i, value, withAutoCompletion, elementTypes);
    } else {
      // Not tested yet
      var editButton = element(by.id('complexTypeFormLabel' + path + i + 'editbutton'));
      expect(editButton.isPresent()).toBe(true);
      editButton.click();
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
  var treeLabel = element(by.id('treeForm' + path + 'label'));
  treeLabel.click();
  var keyInput = element(by.id('mapTypeForm' + path + 'keyinput'));
  keyInput.clear();
  keyInput.sendKeys(keyValue);
  var addElementButton = element(by.id('mapTypeForm' + path + 'addelementbutton'));
  addElementButton.click();
  if (typeof value === 'string') {
    // Attention for the moment this case is not tested
  } else {
    var editButton = element(by.id('complexTypeFormLabel' + path + keyValue + 'editbutton'));
    expect(editButton.isPresent()).toBe(true);
    editButton.click();
    for (var valueField in value) {
      if (value.hasOwnProperty(valueField)) {
        var autoComplete = withAutoCompletion.hasOwnProperty(valueField) ? withAutoCompletion[valueField] : false;
        sendValueToPrimitive(path + keyValue + valueField, value[valueField], autoComplete, elementType[valueField]);
      }
    }
  }
};

module.exports.addKeyToMap = addKeyToMap;

function sendValueToPrimitive (path, value, withAutoCompletion, elementType) {
  if (elementType === 'tosca') {
    common.sendValueToToscaProperty(path, value);
  } else if (elementType === 'xeditable') {
    common.sendValueToXEditable('primitiveTypeFormLabel' + path + 'input', value, withAutoCompletion);
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

function sendValueToPrimitiveSelect(path, value) {
  var selectElement = element(by.id('primitiveTypeFormLabel' + path + 'input'));
  common.chooseSelectOption(selectElement, value);
}

module.exports.sendValueToPrimitiveSelect = sendValueToPrimitiveSelect;

function sendValueToPrimitiveRadio(path, value) {
  var radioElement = element(by.id('primitiveTypeFormLabel' + path + value));
  radioElement.click();
}

module.exports.sendValueToPrimitiveRadio = sendValueToPrimitiveRadio;

function sendValueToPrimitiveInput(path, value) {
  var inputElement = element(by.id('primitiveTypeFormLabel' + path + 'input')).element(by.tagName('input'));
  inputElement.clear();
  inputElement.sendKeys(value);
}

module.exports.sendValueToPrimitiveInput = sendValueToPrimitiveInput;

/*
 Verify that action has been well taken into account
 */
function expectValueFromMap(path, keyValue, value, elementTypes) {
  var treeLabel = element(by.id('treeForm' + path + 'label'));
  treeLabel.click();
  if (typeof value === 'string') {
    // Attention for the moment this case is not tested
  } else {
    var editButton = element(by.id('complexTypeFormLabel' + path + keyValue + 'editbutton'));
    expect(editButton.isPresent()).toBe(true);
    editButton.click();
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
    var treeLabel = element(by.id('treeForm' + path + 'label'));
    treeLabel.click();
    if (typeof value === 'string') {
      expectValueFromPrimitive(path + i, value, elementType);
    } else {
      // Attention for the moment this case is not tested
    }
  }
}

module.exports.expectValueFromArray = expectValueFromArray;

function expectValueFromPrimitive(path, value, elementType) {
  if (elementType === 'tosca') {
    common.expectValueFromToscaProperty(path, value);
  } else if (elementType === 'xeditable') {
    common.expectValueFromXEditable('primitiveTypeFormLabel' + path + 'input', value);
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

function expectValueFromSelectPrimitive(path, value) {
  var selectElement = element(by.id('primitiveTypeFormLabel' + path + 'input'));
  expect(selectElement.getText()).toContain(value);
}

module.exports.expectValueFromSelectPrimitive = expectValueFromSelectPrimitive;

function expectValueFromRadioPrimitive(path, value) {
  var radioElement = element(by.id('primitiveTypeFormLabel' + path + value));
  expect(radioElement.getAttribute('class')).toMatch('active');
}

module.exports.expectValueFromSelectPrimitive = expectValueFromSelectPrimitive;

function abortInput(path) {
  var input = element(by.id('primitiveTypeFormLabel' + path + 'input')).element(by.tagName('input'));
  input.sendKeys(protractor.Key.ESCAPE);
  browser.waitForAngular();
}
module.exports.abortInput = abortInput;

function expectConstraintAlertPresent(path) {
  var warnElement = element(by.id('primitiveTypeFormLabel' + path + 'input')).element(by.css('.text-danger'));
  expect(warnElement.isPresent()).toBe(true);
}
module.exports.expectConstraintAlertPresent = expectConstraintAlertPresent;

function saveForm() {
  browser.actions().click(browser.element(by.binding('GENERIC_FORM.SAVE'))).perform();
  common.dismissAlertIfPresent();
}
module.exports.saveForm = saveForm;

function cancelForm() {
  browser.actions().click(browser.element(by.binding('GENERIC_FORM.CANCEL'))).perform();
}
module.exports.cancelForm = cancelForm;
