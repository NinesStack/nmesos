require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.2/nmesos-cli-0.2.2.tgz"
  sha256 "b3e9542e37b1cc234f2e96dc371209331aa8ec2399d4d14bae689ce062295a4a"

  def install
    bin.install 'nmesos'
  end
end