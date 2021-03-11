# URL to be used for creating, subscribing and reading data
export BASE_URL=http://localhost:8080/FROST-Server/v1.0/

# mqtt broker address
# set BROKER=192.168.99.100
export BROKER=localhost

# Benchmark Session Identifier within Benchmark thing to be used
export SESSION=0815

# Percentage of Datastreams covered by mqtt subsribers
export COVERAGE=50

# number of parallel Sensors data providers
export WORKERS=20

# msec delay between observations
export POSTDELAY=10

java -jar ./StreamProcessor/target/StreamProcessor-1.1-SNAPSHOT-jar-with-dependencies.jar
