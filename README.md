# FROST-Benchmark

A set of tools to run load-tests on a SensorThings API compatible service.

## Use

FROST-Benchmark consists of several parts:
* Controller: The central controller that starts and stops the other components, and manages the main benchmark "Thing" in that is used
  for communication between the components.
* SensorCluster: The component that simulates sensors and posts Observations.
* SubscriberCluster: (Work in progress) A component that subscribes to Datastreams over MQTT.
* StreamProcessor: (Work in progress) A component that subscribes to Datastreams over MQTT and runs processed based on the received data.

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
