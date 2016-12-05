## Brew installer for NMesos CLI tool

This tap contains a formulae to install nmesos cli tool.

## Usage

Install:
```
brew  -v tap nitro/nmesos  git@github.com:Nitro/nmesos.git
brew install nmesos-cli
```

Update:
```
brew update
brew upgrade  nmesos-cli
```

## Release a new version

* Build a new tgz with `sbt cli/universal:packageZipTarball`

* Update the Formula `nmesos-cli.rb` with the public url and push the changes to the Master branch.
