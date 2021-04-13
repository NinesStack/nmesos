# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos/3.0.1/nmesos-3.0.1.tgz"
  sha256 "1f5aaa62348ce97837d99861d051052f33d144dcf0febc32e1fe053af81c0b88"

  def install
    bin.install 'nmesos'
  end
end

