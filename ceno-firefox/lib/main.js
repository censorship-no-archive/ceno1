var { ToggleButton } = require('sdk/ui/button/toggle');
var panels = require('sdk/panel');
var self = require('sdk/self');
var tabs = require('sdk/tabs');

var button = ToggleButton({
  id: 'ceno-toggle',
  label: 'Visit Mozilla',
  icon: {
    '16': './icon-16.png',
    '32': './icon-32.png',
    '64': './icon-64.png',
  },
  onChange: handleChange
});

var panel = panels.Panel({
  contentURL: self.data.url('panel.html'),
  onHide: handleHide
});

function handleChange(state) {
  if (state.checked) {
    panel.show({ position: button });
  }
}

function handleHide() {
  button.state('window', { checked: false });
}
