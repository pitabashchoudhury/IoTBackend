package com.foodchain.iotbackend.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${app.mqtt.broker-url}")
    private String brokerUrl;

    @Value("${app.mqtt.client-id}")
    private String clientId;

    @Value("${app.mqtt.keep-alive-interval}")
    private int keepAliveInterval;

    @Value("${app.mqtt.connection-timeout}")
    private int connectionTimeout;

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setKeepAliveInterval(keepAliveInterval);
        options.setConnectionTimeout(connectionTimeout);
        return options;
    }

    @Bean
    public MqttClient mqttClient(MqttConnectOptions options) throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, clientId + "-" + System.currentTimeMillis(), new MemoryPersistence());
        try {
            client.connect(options);
            log.info("Connected to MQTT broker at {}", brokerUrl);
        } catch (MqttException e) {
            log.warn("Could not connect to MQTT broker at {}. MQTT features will be unavailable: {}", brokerUrl, e.getMessage());
        }
        return client;
    }
}
