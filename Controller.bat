REM URL to be used for creating, subscribing and reading data
set BASE_URL=http://localhost:8080/FROST-Server/v1.0/

REM Benchmark Session Identifier within Benchmark thing to be used
set SESSION=0001

java -jar .\BenchmarkController\target\BenchmarkController-1.1-SNAPSHOT-jar-with-dependencies.jar %1
pause
