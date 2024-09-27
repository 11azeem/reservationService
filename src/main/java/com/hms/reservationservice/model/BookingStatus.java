package com.hms.reservationservice.model;

import java.io.Serializable;

public class BookingStatus implements Serializable {
    private Boolean hotelManagementServiceResponse;
    private Boolean paymentServiceResponse;

    public Boolean getHotelManagementServiceResponse() {
        return hotelManagementServiceResponse;
    }

    public void setHotelManagementServiceResponse(Boolean hotelManagementServiceResponse) {
        this.hotelManagementServiceResponse = hotelManagementServiceResponse;
    }

    public Boolean getPaymentServiceResponse() {
        return paymentServiceResponse;
    }

    public void setPaymentServiceResponse(Boolean paymentServiceResponse) {
        this.paymentServiceResponse = paymentServiceResponse;
    }
}
