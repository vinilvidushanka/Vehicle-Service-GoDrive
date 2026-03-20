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

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final CloudStorageService cloudStorageService; 
    private final BookingRepository bookingRepository;

    // Constructor Injection එකට CloudStorageService එකතු කරන්න
    public VehicleController(VehicleRepository vehicleRepository, 
                             CloudStorageService cloudStorageService, 
                             BookingRepository bookingRepository) {
        this.vehicleRepository = vehicleRepository;
        this.cloudStorageService = cloudStorageService;
        this.bookingRepository = bookingRepository;
    }

    // වාහනයක් පින්තූරයත් සමඟම Save කිරීමේ නව Method එක
    @PostMapping(value = "/save-with-image", consumes = { "multipart/form-data" })
    public ResponseEntity<?> saveVehicle(
            @RequestPart("vehicle") String vehicleJson, // මුලින්ම String එකක් විදිහට ගමු
            @RequestPart("file") MultipartFile file) {
        try {
            // 1. JSON String එක Vehicle Object එකකට හරවන්න (Manual Mapping)
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Vehicle vehicle = objectMapper.readValue(vehicleJson, Vehicle.class);

            // 2. පින්තූරය Upload කරන්න
            String imageUrl = cloudStorageService.uploadFile(file);
            vehicle.setImageUrl(imageUrl); 

            // 3. Save කරන්න
            Vehicle savedVehicle = vehicleRepository.save(vehicle);
            return ResponseEntity.ok(savedVehicle);
            
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // වාහනයක් පින්තූරයත් සමඟම Update කිරීම (PUT Method)
    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<?> updateVehicle(
            @PathVariable long id,
            @RequestPart("vehicle") String vehicleJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            // 1. Database එකේ අදාළ වාහනය ඉන්නවද කියලා බලන්න
            return vehicleRepository.findById(id).map(existingVehicle -> {
                try {
                    // 2. JSON String එක Vehicle Object එකකට හරවන්න
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Vehicle updatedData = objectMapper.readValue(vehicleJson, Vehicle.class);

                    // 3. පරණ දත්ත අලුත් දත්ත වලින් update කරන්න
                    existingVehicle.setBrand(updatedData.getBrand());
                    existingVehicle.setModel(updatedData.getModel());
                    existingVehicle.setVehicleNumber(updatedData.getVehicleNumber());
                    existingVehicle.setFuelType(updatedData.getFuelType());

                    // 4. අලුත් පින්තූරයක් එවා ඇත්නම් පමණක් එය upload කර URL එක update කරන්න
                    if (file != null && !file.isEmpty()) {
                        String newImageUrl = cloudStorageService.uploadFile(file);
                        existingVehicle.setImageUrl(newImageUrl);
                    }

                    // 5. Save කරන්න
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

    // --- පරණ CRUD පියවරවල් ටික පහතින් තබා ගත හැක ---

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

            // මෙතනදී අපි සියලුම booked vehicle IDs ටික String Set එකක් විදිහට ගමු
            Set<String> bookedVehicleIds = bookingsOnDate.stream()
                    .map(booking -> String.valueOf(booking.getVehicleId()))
                    .collect(Collectors.toSet());

            // දැන් Vehicle ID එකත් String එකක් කරලා check කරමු
            List<Vehicle> availableVehicles = allVehicles.stream()
                    .filter(vehicle -> !bookedVehicleIds.contains(String.valueOf(vehicle.getId())))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(availableVehicles);
            
        } catch (Exception e) {
            e.printStackTrace(); // මොකක් හරි error එකක් ආවොත් console එකේ බලාගන්න
            return ResponseEntity.status(500).build();
        }
    }
}