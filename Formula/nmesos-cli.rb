# This file is generated. Do not edit.
# Instead edit and run update.sh.
# You can run update.sh manually or with >sbt updateBrew<.

require 'formula'

class NmesosCli < Formula
  desc "Nmesos is a CLI tool to deploy into Mesos."
  homepage "https://github.com/Nitro/nmesos"
  url "https://nmesos-releases.s3-eu-west-1.amazonaws.com/nitro-public/repo/nitro/nmesos-cli/0.2.23/nmesos-cli-0.2.23.tgz"
  sha256 "c1e7d3f467e47de7c24107e3f2464c773a6a9528d27c6b58776fa1d4a198b568"

  option "with-bash-completion", "Install bash-completion"

  depends_on "bash-completion" => :optional

  def install
    if build.with? "bash-completion"
      bash_completion.install "./contrib/etc/bash_completion.d/nmesos"
    end

    bin.install 'nmesos'
  end
end

