package com.hms.reservationservice;

import com.hms.reservationservice.model.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

public interface BookingDetailRepository extends JpaRepository<BookingDetail, UUID> {
    @Transactional
    @Modifying
    @Query("update BookingDetail b set b.bookingstatus = ?2 where b.id = ?1")
    void updateBookingstatusBy(UUID id, Boolean bookingstatus);

    long countByBookingstatusTrue();
}