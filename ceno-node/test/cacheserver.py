'''This is an implementation of a functional cache server to be used in conjunction with
CeNo Client and a running Transport server on localhost to test the implemnetation of the
latter two.  This program is not meant to run in deployment.
'''

import threading
import socket
import os


class StoreServer(threading.Thread):
  def __init__(self, addr, port):
    threading.Thread.__init__(self)
    self.addr = addr
    self.port = port
    self.terminated = False

  def terminate(self):
    self.terminated = True

  def run(self):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((self.addr, self.port))
    server.listen(1)
    while not self.terminated:
      client, _ = server.accept()
      print('(Cache Server) Client connected (store)')
      store_msg = client.recv(2048)
      if not store_msg.startswith('STORE'):
        print('(Cache Server) Client did not send STORE. Sent ' + store_msg)
        client.send('ERROR expected message STORE\n')
        client.close()
        continue
      # Parse out URL from `STORE <url>\n` message and remove the http(s):// part
      # Note: The real cache servers do not necessarily have to do this. I only do it here
      #       because it makes it easier to store the bundle in a file named after the URL
      url = store_msg.split()[-1][:-1]
      i = url.index('://')
      if i >= 0:
        url = url[i + 3:]
      print('(Cache Server) Got request to store ' + url)
      client.send('READY\n')
      bundle = client.recv(2 ** 31 - 1)
      if bundle.startswith('ERROR'):
        print('(Cache Server) Got error instead of bundle; ' + bundle[bundle.indexOf(' ') + 1:])
      else:
        print('(Cache Server) Got bundle from transport server. Length = ' + str(len(bundle)))
        cache_file = open(os.path.sep.join(['bundles', url]), 'w')
        cache_file.write(bundle)
        cache_file.close()
      client.close()


class LookupServer(threading.Thread):
  def __init__(self, addr, port):
    threading.Thread.__init__(self)
    self.addr = addr
    self.port = port
    self.terminated = False

  def terminate(self):
    self.terminated = True

  def run(self):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((self.addr, self.port))
    server.listen(1)
    while not self.terminated:
      client, _ = server.accept()
      print('(Cache Server) Client connected (lookup)')
      lookup_msg = client.recv(2048)
      if not lookup_msg.startswith('LOOKUP'):
        print('(Cache Server) Client did not send LOOKUP. Sent ' + lookup_msg)
        client.send('ERROR expected message LOOKUP')
        client.close()
        continue
      url = lookup_msg.split()[-1][:-1]
      i = url.index('://')
      if i >= 0:
        url = url[i + 3:]
      print('(Cache Server) Got request to lookup ' + url)
      if os.path.exists(os.path.sep.join(['bundles', url])):
        client.send('RESULT found\n')
        ready = client.recv(2048)
        if not ready.startswith('READY'):
          print('(Cache Server) Client did not send READY. Sent ' + ready)
          client.send('ERROR expected message READY\n')
          client.close()
        else:
          bundle = open(os.path.sep.join(['bundles', url])).read()
          client.send(bundle)
          client.close()
      else:
        client.send('RESULT not found\n')
        okay = client.recv(2048)
        if not okay.startswith('OKAY'):
          print('(Cache Server) Client did not send OKAY. Sent ' + okay)
          client.send('ERROR expected message OKAY\n')
          client.close()
        else:
          print('(Cache Server) Client disconnected')
          client.close()


def main():
  # Create a directory to naively store bundles as files in
  if not os.path.isdir('bundles'):
    os.mkdir('bundles')
  # Start our two servers for cache lookup and storage
  lookup_server = LookupServer('localhost', 3091)
  store_server = StoreServer('localhost', 3092)
  lookup_server.start()
  store_server.start()
  print('Running cache lookup server on port 3091')
  print('Running cache store server on port 3092')
  print('Stop execution with `kill ' + str(os.getpid()) + '`')
  # Have the main thread wait until the two servers are terminated
  lookup_server.join()
  store_server.join()


if __name__ == '__main__':
  main()