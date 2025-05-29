package com.example.controller;

import com.example.domain.*;
import com.example.securityConfig.JwtUtil;
import com.example.service.HomeService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

import io.jsonwebtoken.Claims;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = { "http://127.0.0.1:5500", "http://localhost:5500" })
public class Controller {
    private final HomeService service;

    public Controller(HomeService service) {
        this.service = service;
    }

    @PostMapping("/api/signup")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (service.findUserByEmail(user.getEmail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use");
        }

        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        User created = service.createUser(user);
        return ResponseEntity.ok(Map.of(
                "id", created.getId(),
                "username", created.getUsername(),
                "email", created.getEmail()
        ));
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        if (!request.containsKey("email") || !request.containsKey("password")) {
            return ResponseEntity.badRequest().body("Missing email or password");
        }

        String email = request.get("email");
        String rawPassword = request.get("password");

        User user = service.findUserByEmail(email);
        if (user == null || !BCrypt.checkpw(rawPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String token = JwtUtil.generateToken(user.getEmail(), user.getId());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "name", user.getFirstName() + " " + user.getLastName(),
                "email", user.getEmail(),
                "username", user.getUsername()
        ));
    }

    @PostMapping("/api/trip")
    public ResponseEntity<?> createTrip(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> tripData) {

        User user = getUserFromAuth(authHeader);
        if (user == null) return unauthorizedResponse();

        try {
            Trip trip = new Trip(
                    tripData.get("city"),
                    tripData.get("country"),
                    TripType.fromDisplayName(tripData.get("tripType")),
                    LocalDate.parse(tripData.get("startDate")),
                    LocalDate.parse(tripData.get("endDate")),
                    Transport.valueOf(tripData.get("transport")),
                    tripData.get("description"),
                    user
            );
            return ResponseEntity.ok(service.createTrip(trip));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid trip data: " + e.getMessage());
        }
    }

    @PostMapping("/api/trip/{tripId}/picture")
    public ResponseEntity<?> uploadPicture(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long tripId,
            @RequestParam("image") MultipartFile file) throws IOException {

        User user = getUserFromAuth(authHeader);
        if (user == null) return unauthorizedResponse();

        Trip trip = service.findTripById(tripId);
        if (trip == null || !trip.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        Picture picture = new Picture(trip, file.getBytes());
        service.savePicture(picture);

        return ResponseEntity.ok("Picture saved");
    }

    @GetMapping("/api/trip/picture")
    public ResponseEntity<?> getPicture(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long tripId) {

        User user = getUserFromAuth(authHeader);
        if (user == null) return unauthorizedResponse();

        Trip trip = service.findTripById(tripId);
        if (trip == null || !trip.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        Picture picture = service.findPictureByTripId(tripId);
        if (picture == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Picture not found");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(picture.getData(), headers, HttpStatus.OK);
    }

    @GetMapping("/api/stats")
    public ResponseEntity<?> sendStats(
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromAuth(authHeader);
        if (user == null) return unauthorizedResponse();

        return ResponseEntity.ok(service.userTripStats(user));
    }

    @GetMapping("/api/trips")
    public ResponseEntity<?> getTrips(
            @RequestHeader("Authorization") String authHeader) {

        User user = getUserFromAuth(authHeader);
        if (user == null) return unauthorizedResponse();

        return ResponseEntity.ok(service.findTripsByUserId(user));
    }

    @GetMapping("/api/visitedCountries")
    public ResponseEntity<?> getVisitedCities(@RequestHeader("Authorization") String authHeader) {
        User user = getUserFromAuth(authHeader);
        if (user == null) return unauthorizedResponse();

        return ResponseEntity.ok(service.getVisitedCountries(user));
    }

    private User getUserFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = JwtUtil.validateToken(token);
            Long userId = claims.get("userId", Long.class);
            return service.findUserById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<String> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing token");
    }
}
