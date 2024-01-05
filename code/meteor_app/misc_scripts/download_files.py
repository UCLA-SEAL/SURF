
import json
import sys
import requests
import os
import time


# read json from private/findViewById.json
with open('private/init.json') as f:
    data = json.load(f)

# make new directory for the files to be downloaded
if not os.path.exists('full_source'):
    os.mkdir('full_source')

if not os.path.exists('full_source/javax.crypto.Cipher__init'):
    os.mkdir('full_source/javax.crypto.Cipher__init')

# for each item in the json, download the file from github
for item in data:
    url = item['url']
    example_id = item['exampleID']

    # convert github url to raw url
    url = url.replace('github.com', 'raw.githubusercontent.com')
    url = url.replace('/blob', '')
    url = url.replace('/tree', '')


    filename = url.split('/')[-1]
    print('Downloading {}'.format(url))
    # download the file using requests
    r = requests.get(url)
    # write the file to disk in the
    os.mkdir('full_source/javax.crypto.Cipher__init/' + str(example_id)  + '/')
    with open('full_source/javax.crypto.Cipher__init/' + str(example_id)  + '/' + filename, 'wb') as f:
        f.write(r.content)
    
    # sleep for 0.5 second to avoid rate limiting
    time.sleep(0.5)
    



