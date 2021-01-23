from setuptools import setup, find_namespace_packages
import sys

CURRENT_PYTHON = sys.version_info[:2]
REQUIRED_PYTHON = (3, 7)

# Check the current python version
if CURRENT_PYTHON < REQUIRED_PYTHON:
    sys.stderr.write('MDDE requires Python {0[0]}.{0[1]} or higher. You have Python {1[0]}.{1[1]}.'
                     .format(REQUIRED_PYTHON, CURRENT_PYTHON))
    sys.exit(1)

setup(
    name='mdde.integration.ray',
    version='0.7',
    description='Multi-agent Data Distribution Environment: RAY RLlib integration',

    author='Andrey Kharitonov',
    author_email='andrey.kharitonov@ovgu.de',

    license='MIT Licence',
    packages=find_namespace_packages(include=['mdde.integration.ray.*'], exclude=['mdde.test.*']),

    install_requires=['gym==0.16.0',
                      'ray[rllib,tune]==1.1.0',
                      # 'tensorflow==1.15.2',
                      'tensorflow-probability==0.7.0',
                      'tabulate~=0.8.6',
                      'requests~=2.22.0',
                      'opencv-python~=4.1.1.26',
                      'psutil~=5.6.3',
                      'lz4~=2.2.1',
                      'setproctitle~=1.1.10',
                      'pandas>=1.1.5',
                      'dm-tree~=0.1.4'],
    zip_safe=False,
)
