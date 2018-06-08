provider "docker" {
  host = "unix:///var/run/docker.sock"
}

resource "docker_container" "kafka" {
  image = "${docker_image.kafka.latest}"
  name  = "kafka"
  ports {
    internal = 2181
    external = 2181
  }
  ports {
    internal = 9092
    external = 9092
  }
  env = [ "ADVERTISED_HOST=localhost"
        , "ADVERTISED_PORT=9092"
        ]
}

resource "docker_image" "kafka" {
  name = "spotify/kafka:latest"
}
