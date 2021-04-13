# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos/1.0.1/nmesos-1.0.1.tgz"
  sha256 "d0c3c22321cdd571b4b1a2319701251bb7588db2843df7c8541b22ff4f0a8d3c"

  def install
    bin.install 'nmesos'
  end
end

