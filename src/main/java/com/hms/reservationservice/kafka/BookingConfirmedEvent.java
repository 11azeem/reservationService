package com.hms.reservationservice.kafka;

import java.util.UUID;

public class BookingConfirmedEvent extends ReservationEvent {
    private String bookingId;

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
}
