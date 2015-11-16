import sys
from bs4 import BeautifulSoup as bs

if len(sys.argv) != 2:
    print 'Usage: python {0} <sources.html>'.format(sys.argv[0])
    sys.exit(1)


html = open(sys.argv[1]).read()
soup = bs(html, 'html.parser')
table = [t for t in soup.find_all('table') if t.get('id', '') == 'discovery'][0]
rows = table.tbody.find_all('tr')

output = open('sources.list', 'w')
for row in rows:
    _, _, _, _, feed_url, feed_type, _ = row.find_all('td')
    url = feed_url.a.text
    _type = feed_type.text
    if _type.lower() == 'html':
        output.write(url + '\n')
        print url

output.close()
