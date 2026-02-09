package com.foodchain.iotbackend.repository;

import com.foodchain.iotbackend.entity.DeviceControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeviceControlRepository extends JpaRepository<DeviceControlEntity, UUID> {
}
