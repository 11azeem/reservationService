package com.hms.reservationservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class KafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducer.class);
    private static final String TOPIC = "reservation-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void pubBookingConfirmedEvent(UUID bookingId) throws JsonProcessingException {
        BookingConfirmedEvent bookingConfirmedEvent = new BookingConfirmedEvent();
        bookingConfirmedEvent.setEventType("BOOKING_CONFIRMED");
        bookingConfirmedEvent.setBookingId(String.valueOf(bookingId));

        // convert to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String datum = objectMapper.writeValueAsString(bookingConfirmedEvent);

        logger.info(String.format("#### -> Producing message -> %s", datum));
        this.kafkaTemplate.send(TOPIC, "reservation-key-1", datum);
    }
}
