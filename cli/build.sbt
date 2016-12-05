// Assembly as a CLI auto runnable bin.

scalaVersion := "2.12.0"

val cli = Seq(
  "#!/usr/bin/env sh",
  """exec java -Djava.compiler=NONE -noverify -jar "$0" "$@""""
)

mainClass in assembly := Some("com.nitro.nmesos.cli.Main")
assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(cli))
assemblyJarName in assembly := "nmesos"

