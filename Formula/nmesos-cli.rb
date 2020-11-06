require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.21/nmesos-cli-0.2.21.tgz"
  sha256 "9b406f4147c4061f6bc66e497b187fdb3f3078a6bcc966b73c6ce5505fb1a968"

  option "with-bash-completion", "Install bash-completion"

  depends_on "bash-completion" => :optional

  def install
    if build.with? "bash-completion"
      bash_completion.install "./contrib/etc/bash_completion.d/nmesos"
    end

    bin.install 'nmesos'
  end
end
