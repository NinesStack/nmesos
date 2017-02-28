require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.0.12/nmesos-cli-0.0.12.tgz"

  def install
    bin.install 'nmesos'
  end
end