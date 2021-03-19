#!/bin/sh
#

export FROST_SERVER=http://192.168.178.45:8080/FROST-Server/v1.0/
export MQTT_BROKER=tcp://192.168.178.45:1883
export CONTROLLER_SCRIPTS=./

docker-compose -f docker-compose-sensor-ramp-up.yaml up
