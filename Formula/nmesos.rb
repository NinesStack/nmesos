# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/NinesStack/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/ninesstack/nmesos/3.0.5/nmesos-3.0.5.tgz"
  sha256 "be96ae42fcedea8c86247f97cfb6b0e9698702fc706a52814242e71f9831f535"

  def install
    bin.install 'nmesos'
  end
end

