#!/bin/bash

sm --start DATASTREAM

sbt "run -Drun.mode=Dev -Dhttp.port=11117 $*"
