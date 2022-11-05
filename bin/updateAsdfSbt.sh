#!/bin/bash

prefix=" - "
echo "${prefix}Updating asdf versions ..."

version=${1}
echo "${prefix}Version: ${version}"

echo "${version}" >> ./bin/versions.txt && cat ./bin/versions.txt | sort | uniq > /tmp/versions.txt && cp /tmp/versions.txt ./bin/versions.txt

echo "${prefix}... done!"
