// Assembly as a CLI auto runnable bin.
mainClass in assembly := Some("com.nitro.nmesos.cli.Main")

val cli = Seq(
  "#!/usr/bin/env sh",
  """exec java -Djava.compiler=NONE -noverify -jar "$0" "$@""""
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(cli))
assemblyJarName in assembly := "nmesos"

