#!/bin/bash

function usage 
{
	echo -e "\nuse:  ${0} start|stop|clean \n\n"
}

if [[ ! ${1} ]] ; then usage ; exit 99 ; fi
MODE=${1}

PROJECT_DIR="$(pwd)"
SG_DIR="${PROJECT_DIR}/tmp"
SG_URL="http://packages.couchbase.com/releases/couchbase-sync-gateway/1.0.3/couchbase-sync-gateway-community_1.0.3_x86_64.tar.gz"
SG_PKG="${SG_DIR}/sync_gateway.tar.gz"
SG_TAR="${SG_DIR}/couchbase-sync-gateway"
SG_BIN="${SG_TAR}/bin/sync_gateway"
SG_PID="${SG_DIR}/pid"
SG_CFG="${PROJECT_DIR}/Script/sync-gateway-config.json"

function startSyncGateway
{
	if  [[ ! -e ${SG_BIN} ]] 
		then
		cleanSyncGateway
		mkdir ${SG_DIR}
		curl -s -o ${SG_PKG} ${SG_URL}
		tar xvf ${SG_PKG} -C ${SG_DIR}
		rm -f ${SG_PKG}
	fi

	stopSyncGateway

	${SG_BIN} ${SG_CFG} &
	echo $! > ${SG_PID}
}

function stopSyncGateway
{
	if  [[ -e ${SG_PID} ]]
		then
		PID="$(cat ${SG_PID})"
		kill -9 $(cat ${SG_PID})
		rm -f ${SG_PID}
	fi
}

function cleanSyncGateway
{
	stopSyncGateway
	rm -rf ${SG_DIR}
}

MODE=${1}
if [[ ${MODE} =~ 'start' ]]
	then 
	echo "Start SyncGateway ..."
	startSyncGateway
fi

if [[ ${MODE} =~ 'stop' ]]
	then 
	echo "Stop SyncGateway ..."
	stopSyncGateway
fi

if [[ ${MODE} =~ 'clean' ]]
	then 
	echo "Clean SyncGateway ..."
	cleanSyncGateway
fi
