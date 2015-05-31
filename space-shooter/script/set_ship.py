#!/usr/bin/python

import sys
import json
import httplib
import time

time.sleep(3)
c = httplib.HTTPConnection('127.0.0.1:4984')
c.request("GET", "/spaceshooter/player_data")
response = c.getresponse()
data = json.loads(response.read())
data["ship_data"] = sys.argv[1]
rev = data["_rev"]
data.pop("_rev", None)
c.request("PUT", "/spaceshooter/player_data?rev=" + rev, json.dumps(data))
response = c.getresponse()
print response.status, response.reason
