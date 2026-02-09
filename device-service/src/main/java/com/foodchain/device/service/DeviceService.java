package com.foodchain.device.service;

import com.foodchain.common.dto.device.*;
import com.foodchain.common.exception.ResourceNotFoundException;
import com.foodchain.device.entity.DeviceEntity;
import com.foodchain.device.mapper.DeviceMapper;
import com.foodchain.device.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    public DeviceService(DeviceRepository deviceRepository,
                         DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
    }

    public List<DeviceDto> getAllDevices(UUID userId) {
        return deviceRepository.findAllByUserId(userId).stream()
                .map(deviceMapper::toDto)
                .toList();
    }

    public DeviceDto getDevice(UUID deviceId, UUID userId) {
        DeviceEntity device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        return deviceMapper.toDto(device);
    }

    @Transactional
    public DeviceDto createDevice(CreateDeviceRequest request, UUID userId) {
        DeviceEntity device = new DeviceEntity();
        device.setUserId(userId);
        deviceMapper.applyCreateRequest(request, device);

        device = deviceRepository.save(device);
        return deviceMapper.toDto(device);
    }

    @Transactional
    public DeviceDto updateDevice(UUID deviceId, UpdateDeviceRequest request, UUID userId) {
        DeviceEntity device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));

        deviceMapper.applyUpdateRequest(request, device);

        device = deviceRepository.save(device);
        return deviceMapper.toDto(device);
    }

    @Transactional
    public void deleteDevice(UUID deviceId, UUID userId) {
        DeviceEntity device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        deviceRepository.delete(device);
    }
}
