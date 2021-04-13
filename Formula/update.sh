#!/bin/bash

prefix=" - "
echo "${prefix}Updating brew formula ..."

version=${1}
echo "${prefix}Version: ${version}"

file=./target/universal/nmesos-${version}.tgz
echo "${prefix}File: ${file}"

sha=$(shasum -a 256 ${file} | cut -d\  -f 1)
echo "${prefix}Sha: ${sha}"

url=https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos/${version}/nmesos-${version}.tgz
echo "${prefix}Url: ${url}"

formula=./Formula/nmesos.rb
echo "${prefix}Generating ${formula} ..."

cat > ${formula} << EOF
# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "${url}"
  sha256 "${sha}"

  def install
    bin.install 'nmesos'
  end
end

EOF

echo "${prefix}... done!"
