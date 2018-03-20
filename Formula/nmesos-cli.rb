require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.1.6/nmesos-cli-0.1.6.tgz"
  sha256 "464fd37a923f062bdb9ee0ee414cc11010018765b0f501d8c5b125b85e5052d1"

  def install
    bin.install 'nmesos'
  end
end