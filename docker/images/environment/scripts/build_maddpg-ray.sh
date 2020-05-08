#!/bin/bash

# Build base MDDE-RAY image
docker build -t mdde/env/ray-base:latest -f ../base-ray.Dockerfile ../../../../mdde --no-cache
# Build MDDE-RAY-DQN image
docker build -t mdde/env/ray-maddpg:latest -f ../maddpg-ray.Dockerfile ../../../../mdde --no-cache
