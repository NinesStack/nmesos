require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.7/nmesos-cli-0.2.7.tgz"
  sha256 "c456e01c279bd4d7bd68781411267811da637ad9e7d5c99c2f33c57a30c49582"

  def install
    bin.install 'nmesos'
    bash_completion.install 'contrib/etc/bash_completion.d/nmesos'
  end
end