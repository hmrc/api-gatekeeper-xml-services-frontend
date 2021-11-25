#!/bin/bash

sm --start ASSETS_FRONTEND -r 3.11.0

sm --start MONGO DATASTREAM 

sm --start API_PLATFORM_XML_SERVICES

./run_local.sh
