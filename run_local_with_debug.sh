#!/bin/bash

sm2 --start DATASTREAM

sbt -jvm-debug 5005 "run -Dhttp.port=11117 $*"
