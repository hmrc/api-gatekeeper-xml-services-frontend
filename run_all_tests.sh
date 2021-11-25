#!/usr/bin/env bash
sbt clean compile coverage scalastyle test it:test coverageReport
