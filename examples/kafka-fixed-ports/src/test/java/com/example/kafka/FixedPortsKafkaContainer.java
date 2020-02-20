package com.example.kafka;

import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.KafkaContainer;

public class FixedPortsKafkaContainer extends KafkaContainer   {

    public KafkaContainer withFixedExposedPort(int hostPort, int containerPort) {
        return withFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP);
    }

    public KafkaContainer withFixedExposedPort(int hostPort, int containerPort, InternetProtocol protocol) {
        super.addFixedExposedPort(hostPort, containerPort, protocol);
        return self();
    }
}
