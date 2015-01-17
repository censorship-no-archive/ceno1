var request = require('request');
//var request = require('superagent');

var url = 'https://s.imgur.com/min/global.js?1421364572';

request(url, function (error, response, body) {
//request.get(url).end(function (err, res) {
  console.log(body);
  //console.log(res.body.toString('base64'));
  //console.log(res);
});
