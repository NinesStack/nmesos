require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/com/nitro/nmesos/cli/0.1.0/cli-0.1.0.tgz"

  def install
    bin.install 'nmesos'
  end
end