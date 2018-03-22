require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.0/nmesos-cli-0.2.0.tgz"
  sha256 "ef8b37f58bbd509638e8c28fbeea13ed2f7d8567341aa5a855bbdccc5f370db4"

  def install
    bin.install 'nmesos'
  end
end