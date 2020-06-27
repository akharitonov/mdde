#!/bin/sh

# Prfix for container names
PFX=${1:-""}

# *.env file used by docker-compose
COMPOSE_DIR=../docker/compositions/redis


# With do-nothing
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_dn)

# Without do-nothing
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn)

# With do-nothing, disregard storage
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_dn_sm0)

# With do-nothing, disregard storage
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_sm0)

# With do-nothing, disregard storage
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_dn_sm0_b1)

# With do-nothing, disregard storage
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_sm0_b1)

# Without do-nothing, disregard storage, bench at every step, 1e7 replay buffer
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_sm0_b1_10mrb)

# With do-nothing, consider storage, 80 fragments
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_f80)

# With do-nothing, consider storage, 80 fragments, bench at every step
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_b1_f80)

# With do-nothing, consider storage, 80 fragments, bench at every step, batch size 1000, train batch 4000
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_b1_f80_stm)


# With do-nothing, ignore conflicts
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_dn_ai)

# Without do-nothing, ignore conflicts
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_ai)

# With do-nothing, consider storage, bench at every step,  10000 episodes per 101 step, ignore conflicts
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_dn_b1_e10k_s100_ai)

# Without do-nothing, consider storage, bench at every step, 10000 episodes per 101 step, ignore conflicts
(cd ${COMPOSE_DIR}/scripts/ && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_b1_e10k_s100_ai)


# With do-nothing, gamma=0.5
#(cd ${COMPOSE_DIR}/scripts && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_dn_g05)

# Without do-nothing, gamma=0.5
#(cd ${COMPOSE_DIR}/scripts && sh maddpg_act-mask_stop.sh ${PFX}maddpg_am_wdn_g05)