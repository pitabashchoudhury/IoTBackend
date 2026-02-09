package com.foodchain.iotbackend.service;

import com.foodchain.iotbackend.dto.device.*;
import com.foodchain.iotbackend.entity.DeviceEntity;
import com.foodchain.iotbackend.entity.UserEntity;
import com.foodchain.iotbackend.exception.ResourceNotFoundException;
import com.foodchain.iotbackend.mapper.DeviceMapper;
import com.foodchain.iotbackend.repository.DeviceRepository;
import com.foodchain.iotbackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceMapper deviceMapper;

    public DeviceService(DeviceRepository deviceRepository,
                         UserRepository userRepository,
                         DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
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
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DeviceEntity device = new DeviceEntity();
        device.setUser(user);
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
