package org.codefreak.codefreak.service

import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.HostConfig
import org.codefreak.codefreak.util.DockerUtil

class ContainerBuilder {
  private val hostConfigBuilder = HostConfig.builder()
  private val containerConfigBuilder = ContainerConfig.builder()

  var labels: Map<String, String> = mapOf()
  var name: String? = null
  var image: String? = null
  fun hostConfig(modify: HostConfig.Builder.() -> Unit) = hostConfigBuilder.modify()
  fun containerConfig(modify: ContainerConfig.Builder.() -> Unit) = containerConfigBuilder.modify()
  fun doNothingAndKeepAlive() = containerConfig { entrypoint("tail", "-f", "/dev/null") }

  fun build(): ContainerConfig = containerConfigBuilder
      .hostConfig(hostConfigBuilder.build())
      .labels(labels)
      .also { builder ->
        image?.let { builder.image(DockerUtil.normalizeImageName(it)) }
      }
      .build()
}
