package com.tracking.controller;

import com.tracking.dto.ApiResponse;
import com.tracking.dto.CarrierResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/carriers")
@RequiredArgsConstructor
public class CarrierController{

    private final com.tracking.service.CarrierService carrierService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarrierResponse>>> getAllCarriers() {
        try {
            log.info("REST API: Get all carriers");
            List<CarrierResponse> carriers = carrierService.getAllCarriers();
            return ResponseEntity.ok(ApiResponse.success(carriers));
        } catch (Exception e) {
            log.error("Error getting carriers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<CarrierResponse>> getCarrier(@PathVariable Integer code) {
        try {
            log.info("REST API: Get carrier - {}", code);
            CarrierResponse carrier = carrierService.getCarrier(code);
            return ResponseEntity.ok(ApiResponse.success(carrier));
        } catch (Exception e) {
            log.error("Error getting carrier: {}", code, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncCarriers() {
        try {
            log.info("REST API: Sync carriers from 17-Track");
            carrierService.syncCarriersFromApi();
            return ResponseEntity.ok(ApiResponse.success("Carriers synced successfully", null));
        } catch (Exception e) {
            log.error("Error syncing carriers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
