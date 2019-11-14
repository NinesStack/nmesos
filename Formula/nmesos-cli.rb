require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://s3-us-west-2.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.15/nmesos-cli-0.2.15.tgz"
  sha256 "ffef243f6564709766140de78859a33faaa44338fb1aef20cae5efa44669c702"

  option "with-bash-completion", "Install bash-completion"

  depends_on "bash-completion" => :optional

  def install
    if build.with? "bash-completion"
      bash_completion.install "./contrib/etc/bash_completion.d/nmesos"
    end

    bin.install 'nmesos'
  end
end