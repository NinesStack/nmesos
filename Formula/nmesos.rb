# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class Nmesos < Formula
  desc "Nmesos is a command line tool to deploy into Mesos."
  homepage "https://github.com/NinesStack/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/public/repo/ninesstack/nmesos/3.0.3/nmesos-3.0.3.tgz"
  sha256 "68e3c04105ddfc62ce69cbbd23ce1e4ba7101bae22231acae19482ad818a3e24"

  def install
    bin.install 'nmesos'
  end
end

