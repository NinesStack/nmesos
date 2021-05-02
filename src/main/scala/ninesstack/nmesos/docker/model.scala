package ninesstack.nmesos.docker

object model {

  case class Container(
      id: String,
      image: String,
      name: String,
      host: String,
      env: Map[String, String]
  ) {
    def envTaskId = env.get("TASK_ID")
    def taskId = envTaskId.getOrElse(name.replace("mesos-", ""))
  }

}
