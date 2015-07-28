'''This script is meant to be run to remove extraneous files not needed by the
Chrome and Firefox browser profiles generated for deployment in CENO.
'''

import os
import shutil as sh

# Directory names
ff = './firefox/'
gc = './chrome/'

# Files to keep for the firefox profile
ff_keep = [
  'extensions',
  'extensions.ini',
  'extensions.json',
  'jetpack',
  'key3.db',
  'permissions.sqlite'
]

# Files to keep for the chrome profile
gc_keep = [
  'Extensions',
  'Extension State',
  'Extension Cookies',
  'databases',
  'Local Extension Settings',
  'Preferences',
  'Secure Preferences'
]

def delete_unwanted(directory, to_keep):
  '''Delete all the directories and files in a directory not in a list of items to keep'''
  for f in os.listdir(directory):
    if f not in to_keep:
      path = directory + f
      if os.path.isdir(path):
        print('Removing directory ' + path)
        sh.rmtree(path)
      else:
        print('Removing file ' + path)
        os.remove(path)

delete_unwanted(ff, ff_keep)
delete_unwanted(gc, gc_keep)
