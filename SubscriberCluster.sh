# The name to use for finding our settings
export NAME=Listener_1

# URL to be used for creating, subscribing and reading data
export BASE_URL=http://localhost:8080/FROST-Server/v1.0/

# mqtt broker address
# set BROKER=192.168.99.100
export BROKER=localhost

# Benchmark Session Identifier within Benchmark thing to be used
export SESSION=0815

# Percentage of Datastreams covered by mqtt subsribers
export COVERAGE=50


java -jar ./SubscriberCluster/target/SubscriberCluster-1.1-SNAPSHOT-jar-with-dependencies.jar
