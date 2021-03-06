#!/bin/bash

# For debug
echo "Expected location of MDDE registry: $3:$4"
echo "Expected location of the registry config: $5"


ALL_ARGS=( "$@" )
ALL_ARGS_LEN=${#ALL_ARGS[@]}
ADDITINAL_ARGS=${ALL_ARGS[@]:5:$ALL_ARGS_LEN}
echo "Additional args: ${ADDITINAL_ARGS}"


# Wait for registry to be up and ready before running the script
# Reference: https://docs.docker.com/compose/startup-order/
for i in {0..15}; 
do
    timeout 2 bash -c "</dev/tcp/$3/$4"

    result=$?
    if [ $result -eq 0 ] ; then
        # Registry is reachable
        echo "MDDE Registry is up at: $3:$4"
        python $1 --result-dir $2 --temp-dir /ray_temp --reg-host "$3" --reg-port $4 --env-temp-dir /agents_temp --config "$5" $ADDITINAL_ARGS
        exit 0
    fi
    sleep 3
done
echo "Registry connection timed out" >&2
exit 1
