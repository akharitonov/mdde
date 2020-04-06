#!/bin/bash

MDDE_ROOT=$1

source ~/miniconda/etc/profile.d/conda.sh
conda create -y --name mdde python=3.7
conda activate mdde

#pip install --progress-bar off numpy==1.18.1
pip install --progress-bar off tensorflow-gpu==1.13.2
pip install --progress-bar off psutil
pip install --progress-bar off tabulate==0.8.6
pip install --progress-bar off -U https://s3-us-west-2.amazonaws.com/ray-wheels/latest/ray-0.8.0.dev3-cp36-cp36m-manylinux1_x86_64.whl
pip install --progress-bar off requests


# Install MDDE
cd $MDDE_ROOT
pip install --progress-bar off -e ./core
pip install --progress-bar off -e ./mdde-registry-client-tcp
pip install --progress-bar off -e ./integration-ray

conda deactivate