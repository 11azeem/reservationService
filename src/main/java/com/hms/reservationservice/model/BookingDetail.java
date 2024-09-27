package com.hms.reservationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking_details", schema = "reservation")
public class BookingDetail {
    @Id
    @Column(name = "bookingid", nullable = false)
    private UUID id;

    @Column(name = "customerid")
    private UUID customerid;

    @Column(name = "roomno")
    private Integer roomno;

    @Column(name = "checkindate")
    private LocalDate checkindate;

    @Column(name = "checkoutdate")
    private LocalDate checkoutdate;

    @Column(name = "bookingstatus")
    private Boolean bookingstatus;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerid() {
        return customerid;
    }

    public void setCustomerid(UUID customerid) {
        this.customerid = customerid;
    }

    public Integer getRoomno() {
        return roomno;
    }

    public void setRoomno(Integer roomno) {
        this.roomno = roomno;
    }

    public LocalDate getCheckindate() {
        return checkindate;
    }

    public void setCheckindate(LocalDate checkindate) {
        this.checkindate = checkindate;
    }

    public LocalDate getCheckoutdate() {
        return checkoutdate;
    }

    public void setCheckoutdate(LocalDate checkoutdate) {
        this.checkoutdate = checkoutdate;
    }

    public Boolean getBookingstatus() {
        return bookingstatus;
    }

    public void setBookingstatus(Boolean bookingstatus) {
        this.bookingstatus = bookingstatus;
    }

}