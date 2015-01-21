function runTests(server, tests) {
  var completions = new Array(tests.length);
  for (var i = 0, len = tests.length; i < len; i++) {
    completions[i] = false;
    tests[i](function () {
      completions[i] = true;
    }.bind(undefined, i));
  }
  var id = setTimeout(function () {
    var done = true;
    for (var i = 0, len = tests.length; i < len; i++) {
      if (!completions[i]) {
        done = false;
        break;
      }
    }
    if (done) {
      server.close();
      clearTimeout(id);
    }
  }, 1000);
}

module.exports = runTests;
