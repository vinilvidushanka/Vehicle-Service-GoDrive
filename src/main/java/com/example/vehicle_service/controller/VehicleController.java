package com.example.vehicle_service.controller;

import com.example.vehicle_service.entity.Vehicle;
import com.example.vehicle_service.entity.Booking; // අලුත් path එක
import com.example.vehicle_service.repository.VehicleRepository;
import com.example.vehicle_service.repository.BookingRepository; // අලුත් path එක
import com.example.vehicle_service.service.CloudStorageService; 
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; 

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final CloudStorageService cloudStorageService; 
    private final BookingRepository bookingRepository;

    public VehicleController(VehicleRepository vehicleRepository, 
                             CloudStorageService cloudStorageService, 
                             BookingRepository bookingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.cloudStorageService = cloudStorageService;
        this.bookingRepository = bookingRepository;
    }

    @PostMapping(value = "/save-with-image", consumes = { "multipart/form-data" })
    public ResponseEntity<?> saveVehicle(
            @RequestPart("vehicle") String vehicleJson, 
            @RequestPart("file") MultipartFile file) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Vehicle vehicle = objectMapper.readValue(vehicleJson, Vehicle.class);

            String imageUrl = cloudStorageService.uploadFile(file);
            vehicle.setImageUrl(imageUrl); 

            Vehicle savedVehicle = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(savedVehicle);
            
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<?> updateVehicle(
            @PathVariable long id,
            @RequestPart("vehicle") String vehicleJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            return vehicleRepository.findById(id).map(existingVehicle -> {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Vehicle updatedData = objectMapper.readValue(vehicleJson, Vehicle.class);

                    existingVehicle.setBrand(updatedData.getBrand());
                    existingVehicle.setModel(updatedData.getModel());
                    existingVehicle.setVehicleNumber(updatedData.getVehicleNumber());
                    existingVehicle.setFuelType(updatedData.getFuelType());

                    if (file != null && !file.isEmpty()) {
                        String newImageUrl = cloudStorageService.uploadFile(file);
                        existingVehicle.setImageUrl(newImageUrl);
                    }

                    Vehicle savedVehicle = vehicleRepository.save(existingVehicle);
                    return ResponseEntity.ok(savedVehicle);

                } catch (IOException e) {
                    return ResponseEntity.status(500).body("Error processing data: " + e.getMessage());
                }
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }


    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVehicle(@PathVariable long id) {
        if (!vehicleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        vehicleRepository.deleteById(id);
        return ResponseEntity.ok("Vehicle deleted successfully with id: " + id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable long id) {
        return vehicleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles(@RequestParam String date) {
        try {
            List<Vehicle> allVehicles = vehicleRepository.findAll();
            List<Booking> bookingsOnDate = bookingRepository.findByBookingDate(date);

            Set<String> bookedVehicleIds = bookingsOnDate.stream()
                    .map(booking -> String.valueOf(booking.getVehicleId()))
                    .collect(Collectors.toSet());

            List<Vehicle> availableVehicles = allVehicles.stream()
                    .filter(vehicle -> !bookedVehicleIds.contains(String.valueOf(vehicle.getId())))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(availableVehicles);
            
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(500).build();
        }
    }
}