package com.example.service;

import com.example.domain.User;
import com.example.domain.Trip;
import com.example.repository.UserRepository;
import com.example.repository.TripRepository;
import com.example.domain.Picture;
import com.example.repository.PictureRepository;


import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class HomeService {
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final PictureRepository pictureRepository;

    public HomeService(UserRepository userRepository, TripRepository tripRepository, PictureRepository pictureRepository) {
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.pictureRepository = pictureRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    public List<Trip> findTripsByUserId(User user) {
        Long userId = user.getId();
        return tripRepository.findByUserId(userId).stream().sorted(Comparator.comparing(Trip::getStartDate).reversed()).toList();
    }

    public Trip createTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    public Picture savePicture(Picture picture) {
        return pictureRepository.save(picture);
    }

    public Trip findTripById(Long tripId) {
        return tripRepository.findById(tripId).orElse(null);
    }

    public Picture findPictureByTripId(Long tripId) {
        return pictureRepository.findByTripId(tripId).orElse(null);
    }

    public Map<String, String> userTripStats(User user) {
        List<Trip> trips = findTripsByUserId(user);
        String totalTrips = String.valueOf(trips.size());
        String totalCountries = String.valueOf(trips.stream().map(Trip::getCountry).distinct().count());
        String totalDays = String.valueOf(trips.stream().map(trip -> Period.between(trip.getStartDate(), trip.getEndDate())).mapToInt(period -> period.getDays() + period.getMonths() * 30 + period.getYears() * 365).sum());        
        String worldPercentage =  String.format("%.2f", (trips.stream().map(Trip::getCountry).distinct().count() / 195.0 * 100));

        return Map.of(
            "totalTrips", totalTrips,
            "totalCountries", totalCountries,
            "totalDays", totalDays,
            "worldPercentage", worldPercentage
        );
    }

    public List<String> getVisitedCountries(User user) {
        List<Trip> trips = findTripsByUserId(user);

        return trips.stream().map(Trip::getCountry).distinct().toList();
    }
}
