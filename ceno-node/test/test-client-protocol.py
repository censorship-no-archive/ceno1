'''This script is used to test to ensure that CeNo client
is dealing with its part of the protocol correctly.

We expect to see CeNo client request a bundle for a URL
from the cache and then to request that the Transport Server
produce a new bundle.

Two requests to the same URL with these servers running should
lead to:
1. The Please Wait page being served for the first request
2. The string "Hello world!\n" being served for the second

If a third request is made, the Please Wait page will be served
in response to the third request.
'''

import socket

cacheserver = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
transportserver = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

cacheserver.bind(('localhost', 3091))
cacheserver.listen(1)
transportserver.bind(('localhost', 3093))
transportserver.listen(1)

client, _ = cacheserver.accept()
request = client.recv(2048)
print('(CS) From client: ' + request)
client.send('RESULT not found\n')
okay = client.recv(2048)
print('(CS) From client: ' + okay)
client.close()
cacheserver.close()

client, _ = transportserver.accept()
request = client.recv(2048)
print('(TS) From client: ' + request)
client.send('COMPLETE\n')
ready = client.recv(2048)
print('(TS) From client: ' + ready)
client.send('Hello world!\n')
client.close()
transportserver.close()

print('Done')
