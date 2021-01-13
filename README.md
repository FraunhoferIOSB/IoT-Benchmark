# FROST-Benchmark [![DOI](https://zenodo.org/badge/251536677.svg)](https://zenodo.org/badge/latestdoi/251536677)

A set of tools to run load-tests on a service implementing [SensorThingsAPI Version 1.1 Draft](https://portal.ogc.org/files/92752). 

## Use

FROST-Benchmark consists of several parts:
* Controller: The central controller that starts and stops the other components, and manages the main benchmark "Thing" in that is used
  for communication between the components.
* SensorCluster: The Sensor Cluster application emulates a set of sensors. The sensors are creating observations at a given rate.
* SubscriberCluster: The subscriber cluster implements an application behaviour typical for data consumers. This component subscribes to Datastreams over MQTT.
* StreamProcessor: The stream processor application subscribes to the incoming sensor observations, and the received values are used to trigger new observations. 
* AnalyticsCluster: The analytics application subscribes is used to emulate complex queries which are scheduled in a regular interval.

The other packages are libraries used by these components. There are start-scripts for all components that set the required environment variables
and start the tools. Each of these components needs to run in its own terminal.

1. First start the Controller.

   The Controller has two reqired environment variables:
   * BASE_URL: The url of the SensorThings Service (for example: http://localhost:8080/FROST-Server/v1.0
   * SESSION: The name of the Test-Session.

   After starting, the Controller checks to see if there already is a Thing for the given
   session name. If not, it creates such a thing.

2. Start the SensorCluster.

   The SensorCluster has the following environment variables:
   * NAME: The name of this sensor cluster. Can be used from the controller to send
     new parameters to this cluster specifically.
   * BASE_URL: The url of the SensorThings Service (for example: http://localhost:8080/FROST-Server/v1.0
   * BROKER: The url of the MQTT Broker to use for listening to changes in the main Test-Thing. (for example: tcp://localhost:1883)
   * SESSION: The name of the Test-Session.
   * WORKERS: The number of Threads to use for simulating sensors. This is automatically
     also the maximum number of simultaneous requests the cluster can do to the SensorThings service.
   * SENSORS: The number of Sensors to simulate.
   * PERIOD: The period, in milliseconds, between two generated Observations, per Sensor.

   To get a total Observation frequency of 500 Observations per second, you could set up 1000 Sensors with a PERIOD of 2000, or 100 Sensors
   with a PERIOD of 200, or many other combinations.

3. Run a test. In the Controller, give the command `run 5000`. This will start the SensorCluster, and let it run for 5 seconds.

## License

Copyright (C) 2016 Fraunhofer Institut IOSB, Fraunhoferstr. 1, D 76131
Karlsruhe, Germany.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
