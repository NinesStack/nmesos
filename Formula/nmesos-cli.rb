require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.5/nmesos-cli-0.2.5.tgz"
  sha256 "bd1f5f8cd526323a658ea1432e8259a165fc15ef06453c2a7edbe76826884ebc"

  def install
    bin.install 'nmesos'
  end
end