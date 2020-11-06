#!/bin/bash

version=${1}
echo "Version: ${version}"

file=cli/target/universal/nmesos-cli-${version}.tgz
echo "File: ${file}"

sha=$(shasum -a 256 ${file} | cut -d\  -f 1)
echo "Sha: ${sha}"

url=https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/${version}/nmesos-cli-${version}.tgz
echo "Url: ${url}"

formula=./Formula/nmesos-cli.rb
echo "Generating ${formula} ..."

cat > ${formula} << EOF
# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "${url}"
  sha256 "${sha}"

  option "with-bash-completion", "Install bash-completion"

  depends_on "bash-completion" => :optional

  def install
    if build.with? "bash-completion"
      bash_completion.install "./contrib/etc/bash_completion.d/nmesos"
    end

    bin.install 'nmesos'
  end
end

EOF

echo "... done!"
