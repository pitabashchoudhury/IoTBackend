package com.foodchain.device.controller;

import com.foodchain.common.dto.device.*;
import com.foodchain.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceDto>> getAllDevices(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(deviceService.getAllDevices(UUID.fromString(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable UUID id,
                                               @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(deviceService.getDevice(id, UUID.fromString(userId)));
    }

    @PostMapping
    public ResponseEntity<DeviceDto> createDevice(@Valid @RequestBody CreateDeviceRequest request,
                                                  @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.createDevice(request, UUID.fromString(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable UUID id,
                                                  @RequestBody UpdateDeviceRequest request,
                                                  @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(deviceService.updateDevice(id, request, UUID.fromString(userId)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDevice(@PathVariable UUID id,
                             @RequestHeader("X-User-Id") String userId) {
        deviceService.deleteDevice(id, UUID.fromString(userId));
    }
}
