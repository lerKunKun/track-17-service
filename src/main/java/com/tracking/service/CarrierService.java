package com.tracking.service;

import com.tracking.client.SeventeenTrackClient;
import com.tracking.dto.CarrierResponse;
import com.tracking.dto.TrackApiResponse;
import com.tracking.entity.Carrier;
import com.tracking.repository.CarrierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierService {

    private final CarrierRepository carrierRepository;
    private final SeventeenTrackClient apiClient;
    private final RedisCacheService cacheService;

    @Value("${tracking.cache.carrier-ttl}")
    private long carrierCacheTtl;

    @Transactional(readOnly = true)
    public List<CarrierResponse> getAllCarriers() {
        return carrierRepository.findAllActive().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CarrierResponse getCarrier(Integer carrierCode) {
        // Check cache first
        Optional<CarrierResponse> cached = cacheService
                .getCarrierInfo(carrierCode, CarrierResponse.class);

        if (cached.isPresent()) {
            log.debug("Carrier info retrieved from cache: {}", carrierCode);
            return cached.get();
        }

        // Get from database
        Carrier carrier = carrierRepository.findById(carrierCode)
                .orElseThrow(() -> new RuntimeException("Carrier not found: " + carrierCode));

        CarrierResponse response = convertToResponse(carrier);

        // Cache the result
        cacheService.cacheCarrierInfo(carrierCode, response, carrierCacheTtl);

        return response;
    }

    @Transactional
    public void syncCarriersFromApi() {
        log.info("Syncing carriers from 17-Track API");

        try {
            TrackApiResponse<List<Map<String, Object>>> response = apiClient.getCarriers();

            if (response.getCode() != 0) {
                throw new RuntimeException("API error: " + response.getMessage());
            }

            List<Map<String, Object>> carriersData = response.getData();
            if (carriersData == null || carriersData.isEmpty()) {
                log.warn("No carriers returned from API");
                return;
            }

            int savedCount = 0;
            for (Map<String, Object> carrierData : carriersData) {
                try {
                    Integer carrierCode = (Integer) carrierData.get("carrier");
                    String carrierName = (String) carrierData.get("name");
                    String carrierKey = (String) carrierData.get("key");
                    String country = (String) carrierData.get("country");

                    if (carrierCode == null || carrierName == null) {
                        continue;
                    }

                    Carrier carrier = carrierRepository.findById(carrierCode)
                            .orElse(Carrier.builder()
                                    .carrierCode(carrierCode)
                                    .build());

                    carrier.setCarrierName(carrierName);
                    carrier.setCarrierKey(carrierKey);
                    carrier.setCountry(country);
                    carrier.setIsActive(true);

                    carrierRepository.save(carrier);
                    savedCount++;

                } catch (Exception e) {
                    log.error("Error saving carrier data", e);
                }
            }

            log.info("Successfully synced {} carriers", savedCount);

        } catch (Exception e) {
            log.error("Error syncing carriers from API", e);
            throw new RuntimeException("Failed to sync carriers", e);
        }
    }

    @Transactional
    public void addOrUpdateCarrier(Carrier carrier) {
        carrierRepository.save(carrier);
        log.info("Saved carrier: {} - {}", carrier.getCarrierCode(), carrier.getCarrierName());
    }

    private CarrierResponse convertToResponse(Carrier carrier) {
        return CarrierResponse.builder()
                .carrierCode(carrier.getCarrierCode())
                .carrierName(carrier.getCarrierName())
                .carrierKey(carrier.getCarrierKey())
                .country(carrier.getCountry())
                .phone(carrier.getPhone())
                .website(carrier.getWebsite())
                .logoUrl(carrier.getLogoUrl())
                .build();
    }
}
