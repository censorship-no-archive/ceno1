import socket

cacheserver = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

cacheserver.bind(('localhost', 3092))
cacheserver.listen(1)

client.connect(('localhost', 3093))
client.send('BUNDLE http://google.com/\n')
complete = client.recv(2048)
print('(client) From transport: ' + complete)
client.send('READY\n')
bundle = client.recv(2 ** 16)
print('(client) Got bundle from transport. Length = ', len(bundle))
client.close()

connection, _ = cacheserver.accept()
store = connection.recv(2048)
print('(CS) From transport: ' + store)
connection.send('READY\n')
bundle = connection.recv(2 ** 16)
print('(CS) Got bundle from transport. Length = ', len(bundle))
connection.close()

print('Done')
