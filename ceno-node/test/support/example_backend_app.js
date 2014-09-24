var async = require('async'),
    express = require('express');

var app = express();

app.get('/echo', function(req, res) {
  res.send(req.query.input);
});

app.get('/echo_chunked', function(req, res) {
  var parts = req.query.input.split('');
  async.eachSeries(parts, function(part, next) {
    setTimeout(function() {
      res.write(part);
      next();
    }, 10);
  }, function() {
    setTimeout(function() {
      res.end('');
    }, 10);
  });
});

app.get('/rand', function(req, res) {
  setTimeout(function() {
    var rand = Math.random().toString();
    res.send(rand);
  }, 50);
});

app.listen(9444);
