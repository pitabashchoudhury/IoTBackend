package com.foodchain.iotbackend.repository;

import com.foodchain.iotbackend.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    List<DeviceEntity> findAllByUserId(UUID userId);

    Optional<DeviceEntity> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
