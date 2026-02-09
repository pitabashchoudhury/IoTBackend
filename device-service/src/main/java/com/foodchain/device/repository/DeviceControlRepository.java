package com.foodchain.device.repository;

import com.foodchain.device.entity.DeviceControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeviceControlRepository extends JpaRepository<DeviceControlEntity, UUID> {
}
