# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/NinesStack/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/ninesstack/nmesos/3.0.4/nmesos-3.0.4.tgz"
  sha256 "ff4fb62ab7292913489609abc5dedbfa1ad6cf29a3b43d5d1d30b7afc404b8fb"

  def install
    bin.install 'nmesos'
  end
end

