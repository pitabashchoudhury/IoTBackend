package com.foodchain.iotbackend.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "device_controls")
public class DeviceControlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceEntity device;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false)
    private ControlType controlType;

    @Column(name = "current_value")
    private String currentValue;

    @Column(name = "min_value")
    private Float minValue;

    @Column(name = "max_value")
    private Float maxValue;

    private Float step;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> options;

    @Column(name = "mqtt_topic")
    private String mqttTopic;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DeviceEntity getDevice() { return device; }
    public void setDevice(DeviceEntity device) { this.device = device; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ControlType getControlType() { return controlType; }
    public void setControlType(ControlType controlType) { this.controlType = controlType; }

    public String getCurrentValue() { return currentValue; }
    public void setCurrentValue(String currentValue) { this.currentValue = currentValue; }

    public Float getMinValue() { return minValue; }
    public void setMinValue(Float minValue) { this.minValue = minValue; }

    public Float getMaxValue() { return maxValue; }
    public void setMaxValue(Float maxValue) { this.maxValue = maxValue; }

    public Float getStep() { return step; }
    public void setStep(Float step) { this.step = step; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getMqttTopic() { return mqttTopic; }
    public void setMqttTopic(String mqttTopic) { this.mqttTopic = mqttTopic; }
}
