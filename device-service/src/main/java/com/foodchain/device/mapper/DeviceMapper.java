package com.foodchain.device.mapper;

import com.foodchain.common.dto.device.*;
import com.foodchain.device.entity.ControlType;
import com.foodchain.device.entity.DeviceControlEntity;
import com.foodchain.device.entity.DeviceEntity;
import com.foodchain.device.entity.DeviceType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DeviceMapper {

    public DeviceDto toDto(DeviceEntity entity) {
        DeviceLocationDto location = new DeviceLocationDto(
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getAddress(),
                entity.getLocationLabel()
        );

        List<DeviceControlDto> controls = entity.getControls() != null
                ? entity.getControls().stream().map(this::toControlDto).toList()
                : Collections.emptyList();

        return new DeviceDto(
                entity.getId().toString(),
                entity.getName(),
                entity.getType().name(),
                entity.isOnline(),
                location,
                controls,
                entity.getMqttTopicPrefix(),
                entity.getCreatedAt().toEpochMilli(),
                entity.getUpdatedAt().toEpochMilli()
        );
    }

    public DeviceControlDto toControlDto(DeviceControlEntity entity) {
        return new DeviceControlDto(
                entity.getId().toString(),
                entity.getName(),
                entity.getControlType().name(),
                entity.getCurrentValue(),
                entity.getMinValue(),
                entity.getMaxValue(),
                entity.getStep(),
                entity.getOptions() != null ? entity.getOptions() : Collections.emptyList(),
                entity.getMqttTopic()
        );
    }

    public void applyCreateRequest(CreateDeviceRequest request, DeviceEntity entity) {
        entity.setName(request.name());
        entity.setType(DeviceType.valueOf(request.type()));
        entity.setOnline(false);
        entity.setMqttTopicPrefix(request.mqttTopicPrefix());

        if (request.location() != null) {
            entity.setLatitude(request.location().latitude());
            entity.setLongitude(request.location().longitude());
            entity.setAddress(request.location().address());
            entity.setLocationLabel(request.location().label());
        }

        if (request.controls() != null) {
            List<DeviceControlEntity> controls = request.controls().stream()
                    .map(dto -> toControlEntity(dto, entity))
                    .toList();
            entity.getControls().clear();
            entity.getControls().addAll(controls);
        }
    }

    public void applyUpdateRequest(UpdateDeviceRequest request, DeviceEntity entity) {
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.type() != null) {
            entity.setType(DeviceType.valueOf(request.type()));
        }
        if (request.mqttTopicPrefix() != null) {
            entity.setMqttTopicPrefix(request.mqttTopicPrefix());
        }
        if (request.location() != null) {
            entity.setLatitude(request.location().latitude());
            entity.setLongitude(request.location().longitude());
            entity.setAddress(request.location().address());
            entity.setLocationLabel(request.location().label());
        }
        if (request.controls() != null) {
            entity.getControls().clear();
            List<DeviceControlEntity> controls = request.controls().stream()
                    .map(dto -> toControlEntity(dto, entity))
                    .toList();
            entity.getControls().addAll(controls);
        }
    }

    private DeviceControlEntity toControlEntity(DeviceControlDto dto, DeviceEntity device) {
        DeviceControlEntity entity = new DeviceControlEntity();
        entity.setDevice(device);
        entity.setName(dto.name());
        entity.setControlType(ControlType.valueOf(dto.controlType()));
        entity.setCurrentValue(dto.currentValue());
        entity.setMinValue(dto.minValue());
        entity.setMaxValue(dto.maxValue());
        entity.setStep(dto.step());
        entity.setOptions(dto.options());
        entity.setMqttTopic(dto.mqttTopic());
        return entity;
    }
}
