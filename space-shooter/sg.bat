@echo off
if not exist "C:\Program Files (x86)\Couchbase\sync_gateway.exe" (
    mkdir tmp
    curl.exe http://packages.couchbase.com/builds/mobile/sync_gateway/1.0.4/1.0.4-34/couchbase-sync-gateway-community_1.0.4-34_x86_64.exe -o tmp/sync_gateway.exe
    tmp\sync_gateway.exe /S /v/passive /qn
    rmdir tmp /S /Q
)

"C:\Program Files (x86)\Couchbase\sync_gateway.exe" script\sync-gateway-config.json

