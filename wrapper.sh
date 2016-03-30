#!/bin/bash
# start mongowrapper in console mode
#   this is a wrapper of the wrapper (are you wrapped?)
#
## Usage example
# ./wrapper.sh -h
# ./wrapper.sh -n myDB
#
FINALNAME=mongodbdump-java-wrapper
TARGETJAR=`ls target/${FINALNAME}-*.jar|grep -Ev '(javadoc|sources)'`
if [ ! -e "$TARGETJAR" ]; then
  echo unable to find 'TARGETJAR' : suppose you have done \"mvn clean install\" with success :p
  exit -1
fi
#
java -Dlog4j.configuration=file:log4j.properties -jar ${TARGETJAR} $*
