#!/bin/bash

sm2 --start DATASTREAM

sbt "run -Drun.mode=Dev -Dhttp.port=11117 $*"
