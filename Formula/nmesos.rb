# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/NinesStack/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/ninesstack/nmesos/3.0.6/nmesos-3.0.6.tgz"
  sha256 "283be9fa4c413f635c87b6554076e2c997424af493249ace6ffc52c4b57a2276"

  def install
    bin.install 'nmesos'
  end
end

