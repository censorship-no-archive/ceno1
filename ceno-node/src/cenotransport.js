/* Using this program:
 * This basically starts all the servers necessary to run CeNo in the
 * development/testing/deployment setting you are using it in.
 *
 * To run both the client and transport servers, the cache server must
 * already be running on the correct ports.
 * Simply comment out the start function for any servers you do not
 * want to run.
 */

var client = require('./ceno-node');
var transport = require('./transport-node');
var cacheServer = require('../test/cacheserver');

cacheServer.start();
client.start();
transport.start();