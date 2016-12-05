require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://dl.bintray.com/nitro/nmesos/nmesos-cli-0.1.0.tgz"

  def install
    bin.install 'nmesos'
  end
end