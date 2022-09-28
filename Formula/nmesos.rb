# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/NinesStack/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/ninesstack/nmesos/3.0.6/nmesos-3.0.6.tgz"
  sha256 "29b57053314c3cf9dec755dd598b45cc9bfd625531a8ad0298c60882c44ec25d"

  def install
    bin.install 'nmesos'
  end
end

