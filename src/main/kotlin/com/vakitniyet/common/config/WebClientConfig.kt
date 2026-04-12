package com.vakitniyet.common.config

import io.netty.resolver.dns.DnsAddressResolverGroup
import io.netty.resolver.dns.DnsServerAddresses
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.net.InetSocketAddress

@Configuration
class WebClientConfig {

    @Bean
    fun externalHttpClient(): HttpClient {
        val dnsServers = DnsServerAddresses.sequential(
            InetSocketAddress("8.8.8.8", 53),
            InetSocketAddress("1.1.1.1", 53)
        )
        return HttpClient.create().resolver { spec ->
            spec.dnsAddressResolverGroupProvider { builder ->
                builder.nameServerProvider { _ -> dnsServers.stream() }
                DnsAddressResolverGroup(builder)
            }
        }
    }

    @Bean
    fun externalWebClientBuilder(externalHttpClient: HttpClient): WebClient.Builder =
        WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(externalHttpClient))
}
