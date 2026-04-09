package com.vakitniyet.common.config

import io.lettuce.core.ClientOptions
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DnsResolvers
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {

    @Bean
    fun lettuceClientConfigurationBuilderCustomizer(): LettuceClientConfigurationBuilderCustomizer {
        return LettuceClientConfigurationBuilderCustomizer { builder ->
            builder.clientResources(
                ClientResources.builder()
                    .dnsResolver(DnsResolvers.JVM_DEFAULT)
                    .build()
            )
        }
    }
}
