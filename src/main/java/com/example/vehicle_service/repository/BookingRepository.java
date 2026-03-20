package com.example.vehicle_service.repository;

import com.example.vehicle_service.entity.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByBookingDate(String bookingDate);
}