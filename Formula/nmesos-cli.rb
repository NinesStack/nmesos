require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.6/nmesos-cli-0.2.6.tgz"
  sha256 "381012157e462455a4461014e8ed3603966ae0aa36da4a0c64d603442c576bd2"

  def install
    bin.install 'nmesos'
  end
end