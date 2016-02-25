#!/bin/bash
java -Dlog4j.configuration=file:log4j.properties -jar target/mongodbdump-java-wrapper.jar $*
