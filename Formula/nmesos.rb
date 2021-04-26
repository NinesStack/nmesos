# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos/3.0.2/nmesos-3.0.2.tgz"
  sha256 "c9b9ba32c8fd8caab89fe36e4767ba57266969fb7073344b32fb469ee5333a78"

  def install
    bin.install 'nmesos'
  end
end

