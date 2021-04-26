#!/bin/bash

prefix=" - "
echo "${prefix}Updating asdf versions ..."

version=${1}
echo "${prefix}Version: ${version}"

echo "${version}" >> ./bin/versions.txt

echo "${prefix}... done!"
