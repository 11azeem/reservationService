package com.hms.reservationservice;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Autowired
    private BookingDetailRepository bookingDetailRepository;

    @Bean
    public Gauge registeredUsers(MeterRegistry registry) {
        return Gauge.builder("completed-bookings", bookingDetailRepository::countByBookingstatusTrue)
                .description("The number of completed bookings")
                .tag("type", "custom-metric")
                .register(registry);
    }
}
