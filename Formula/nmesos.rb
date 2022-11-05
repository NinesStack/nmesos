# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/NinesStack/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/ninesstack/nmesos/3.0.7/nmesos-3.0.7.tgz"
  sha256 "457dc05fe118a9d7146fd8578d539ffa4aa521350ac5ddae3fcee5ab1be08a8c"

  def install
    bin.install 'nmesos'
  end
end

