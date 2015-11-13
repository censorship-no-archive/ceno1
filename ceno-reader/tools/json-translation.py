'''Prepare the CENO Client's content for translation.
goi18n expects JSON files for translation to be of the following form:

[
  {
    "id": "some_identifier",
    "translation": "Text in some language"
  }
]

However Transifex's JSON format is a simple

{
  "some_identifier": "Text in some language"
}

Format.  This utility converts the existing en-us.json file into the format preferred
by Transifex and back.

To convert all the *.untranslated.json files generated from the *.all.json files by goi18n

    python json-translation.py to

To convert back to the goi18n format once the generated *.transifex.json files have been filled

    python json-translation.py from
'''

import os
import sys
import json
import re

# The directory translation files are stored in
BASE_DIR = '../translations/'

# Regular expression matching untranslated files
UNTRANSLATED_REGEXP = re.compile('[a-z]+-[a-z]+\.untranslated\.json')

# Regular expression matching translated files
TRANSIFEX_REGEXP = re.compile('[a-z]+-[a-z]+\.transifex\.json')


def is_untranslated(filename):
  '''Determine if a file is one of *.untranslated.json'''
  return UNTRANSLATED_REGEXP.match(filename) is not None


def is_transifex(filename):
  '''Determine if a file is one of *.transifex.json'''
  return TRANSIFEX_REGEXP.match(filename) is not None


def convert_to_transifex():
  '''Convert the .untranslated.json files' contents to .transifex.json files with Transifex's formatting'''
  to_convert = [fn for fn in os.listdir(BASE_DIR) if is_untranslated(fn)]
  for filename in to_convert:
    print('Converting', filename)
    f = open(BASE_DIR + filename, 'r')
    content = json.load(f)
    f.close()
    id_texts = {}
    for pair in content:
      id_texts[pair['id']] = pair['translation']
    transifex_file = BASE_DIR + filename.replace('untranslated', 'transifex', 1)
    json.dump(id_texts, open(transifex_file, 'w'), indent=4)


def convert_from_transifex():
  '''Convert translated files .transifex.json from transifex's format to goi18n's format .untranslation.json'''
  to_convert = [fn for fn in os.listdir(BASE_DIR) if is_transifex(fn)]
  for filename in to_convert:
    print('Converting', filename)
    content = json.load(open(BASE_DIR + filename, 'r'))
    pairs = []
    for identifier in content.keys():
      pairs.append({'id': identifier, 'translation': content[identifier]})
    untranslated_file = BASE_DIR + filename.replace('transifex', 'untranslated', 1)
    json.dump(pairs, open(untranslated_file, 'w'), indent=4)


def main():
  if len(sys.argv) < 2:
    print('Run as: python {0} <to|from>'.format(sys.argv[0]))
    sys.exit(1)
  if sys.argv[1] == 'to':
    convert_to_transifex()
  else:
    convert_from_transifex()


if __name__ == '__main__':
  main()
