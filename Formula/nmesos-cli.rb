require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.1/nmesos-cli-0.2.1.tgz"
  sha256 "e389ec995f14c74cdcff28d7fb92d88671bcf36c242aebcb35b7b9b4b1f44ca5"

  def install
    bin.install 'nmesos'
  end
end