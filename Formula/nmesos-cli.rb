require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "curl https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.18/nmesos-cli-0.2.18.tgz"
  sha256 "17597c49b696f53a3e8b762a95f4caa75f589996fc434a66fd41b7116bf53d4e"

  option "with-bash-completion", "Install bash-completion"

  depends_on "bash-completion" => :optional

  def install
    if build.with? "bash-completion"
      bash_completion.install "./contrib/etc/bash_completion.d/nmesos"
    end

    bin.install 'nmesos'
  end
end
