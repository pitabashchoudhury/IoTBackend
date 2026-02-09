package com.foodchain.iotbackend.controller;

import com.foodchain.iotbackend.dto.device.*;
import com.foodchain.iotbackend.security.UserPrincipal;
import com.foodchain.iotbackend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<List<DeviceDto>> getAllDevices(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(deviceService.getAllDevices(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable UUID id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(deviceService.getDevice(id, principal.getId()));
    }

    @PostMapping
    public ResponseEntity<DeviceDto> createDevice(@Valid @RequestBody CreateDeviceRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.createDevice(request, principal.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable UUID id,
                                                  @RequestBody UpdateDeviceRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(deviceService.updateDevice(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDevice(@PathVariable UUID id,
                             @AuthenticationPrincipal UserPrincipal principal) {
        deviceService.deleteDevice(id, principal.getId());
    }
}
