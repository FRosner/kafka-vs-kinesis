provider "kafka" {
  bootstrap_servers = ["localhost:9092"]
  skip_tls_verify   = true
}

resource "kafka_topic" "logs" {
  name               = "logs"
  replication_factor = 1
  partitions         = 2

  config = {
    "segment.ms"     = "20000"
    "cleanup.policy" = "compact"
  }
}
