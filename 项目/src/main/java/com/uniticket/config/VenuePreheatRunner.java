package com.uniticket.config;

import com.uniticket.entity.Venue;
import com.uniticket.service.impl.VenueServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class VenuePreheatRunner implements CommandLineRunner {

    private static final long LOGIC_EXPIRE_SECONDS = 3600L;

    @Resource
    private VenueServiceImpl venueService;

    @Override
    public void run(String... args) {
        List<Venue> venues = venueService.list();
        if (venues == null || venues.isEmpty()) {
            log.info("Venue preheat skipped: no venues found.");
            return;
        }
        for (Venue venue : venues) {
            if (venue.getId() == null) {
                continue;
            }
            venueService.saveVenue2Redis(venue.getId(), LOGIC_EXPIRE_SECONDS);
        }
        log.info("Venue preheat completed: {} venues.", venues.size());
    }
}
