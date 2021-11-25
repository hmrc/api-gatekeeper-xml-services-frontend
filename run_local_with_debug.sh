#!/bin/bash

sm --start DATASTREAM

sbt -jvm-debug 5005 "run -Dhttp.port=11117 $*"
