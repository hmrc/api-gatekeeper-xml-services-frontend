#!/bin/bash

sm2 --start DATASTREAM

sbt "run -Drun.mode=Dev -Dapplication.router=testOnlyDoNotUseInAppConf.Routes -Dhttp.port=11117 $*"
