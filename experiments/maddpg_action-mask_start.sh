#!/bin/sh

# Prfix for container names
PFX=${1:-""}

# *.env file used by docker-compose
ARGS_FILE=args.env
COMPOSE_DIR=../docker/compositions/redis

# Build required images 
(cd ${COMPOSE_DIR}/scripts && sh maddpg_build.sh)

# With do-nothing
rm ${COMPOSE_DIR}/${ARGS_FILE}
echo "" > ${COMPOSE_DIR}/${ARGS_FILE}
(cd ${COMPOSE_DIR}/scripts && sh maddpg_act-mask_start_detached.sh ${PFX}maddpg_am_dn)

# Without do-nothing
rm ${COMPOSE_DIR}/${ARGS_FILE}
echo "LAUNCH_ARGS=--no-do-nothing" > ${COMPOSE_DIR}/${ARGS_FILE}
(cd ${COMPOSE_DIR}/scripts && sh maddpg_act-mask_start_detached.sh ${PFX}maddpg_am_wdn)

# With do-nothing, gamma=0.5
#rm ${COMPOSE_DIR}/${ARGS_FILE}
#echo "LAUNCH_ARGS=--gamma 0.5" > ${COMPOSE_DIR}/${ARGS_FILE}
#(cd ${COMPOSE_DIR}/scripts && sh maddpg_act-mask_start_detached.sh ${PFX}maddpg_dn_g05)

# Without do-nothing, gamma=0.5
#rm ${COMPOSE_DIR}/${ARGS_FILE}
#echo "LAUNCH_ARGS=--no-do-nothing --gamma 0.5" > ${COMPOSE_DIR}/${ARGS_FILE}
#(cd ${COMPOSE_DIR}/scripts && sh maddpg_act-mask_start_detached.sh ${PFX}maddpg_wdn_g05)

# Cleanup the args file
rm ${COMPOSE_DIR}/${ARGS_FILE}
echo "" > ${COMPOSE_DIR}/${ARGS_FILE}