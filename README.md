# Instructions

First build the project with Apache Maven:

    mvn package

The file `target/benchmarks.jar` is a self-contained executable Jar for running JMH benchmarks.

To get help:

    java -jar target/benchmarks.jar -h

To list all benchmarks:

    java -jar target/benchmarks.jar -l

To run a specific benchmark, use a command such as:

    java -jar target/benchmarks.jar -f 3 -wi 100 -i 20 '.*TextProcessing.*'

