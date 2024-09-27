package com.hms.reservationservice.interfaceConfig;

import com.hms.reservationservice.logging.LoggingWebClientFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
public class PaymentServiceConfig {
    private final DiscoveryClient discoveryClient;
    private static final String PAYMENT_SERVICE_HOSTNAME = "paymentService";

    @Autowired
    public PaymentServiceConfig(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Bean
    public WebClient paymentServiceClientGetPaymentStatus(WebClient.Builder webClientBuilder) {

        ServiceInstance instance = getServiceInstance(PAYMENT_SERVICE_HOSTNAME);
        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder
                .baseUrl(String.format("http://%s:%d/api/v1/%s", hostname, port, "getPaymentStatus"))
                .filter(new LoggingWebClientFilter())
                .build();

    }

    public ServiceInstance getServiceInstance(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            throw new RuntimeException("No instances found for " + serviceName);
        }
        return instances.get(0);
    }
}
