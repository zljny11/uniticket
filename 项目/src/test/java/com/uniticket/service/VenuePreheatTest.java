package com.uniticket.service;


import com.uniticket.entity.Venue;
import com.uniticket.service.impl.VenueServiceImpl;

import com.uniticket.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.uniticket.utils.RedisConstants.CACHE_VENUE_KEY;


@SpringBootTest
class VenuePreheatTest {

    @Resource
    private VenueServiceImpl venueService;

    @Resource
    private CacheClient cacheClient;

    @Test
    void testSaveVenue() throws InterruptedException {
        Venue venue = venueService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_VENUE_KEY + 1L, venue, 10L, TimeUnit.SECONDS);

    }
}
