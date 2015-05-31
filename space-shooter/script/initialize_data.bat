@echo off
..\curl.exe -s -X PUT -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d @alternateship_data.json http://127.0.0.1:4984/spaceshooter/alternateship
..\curl.exe -s -X PUT -H "Content-Type: application/octet-stream" -H "Cache-Control: no-cache" --data-binary @alternateship "http://127.0.0.1:4984/spaceshooter/alternateship/data?rev=1-1c3545754e1ee09226444d361e17840a"

