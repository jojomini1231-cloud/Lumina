package com.lumina.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SnowflakeIdGenerator {
    private final long datacenterId;
    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${lumina.snowflake.datacenter-id:-1}") long configDatacenterId,
            @Value("${lumina.snowflake.machine-id:-1}") long configMachineId) {
        
        if (configDatacenterId >= 0 && configDatacenterId <= 31) {
            this.datacenterId = configDatacenterId;
        } else {
            this.datacenterId = generateDatacenterId();
        }

        if (configMachineId >= 0 && configMachineId <= 31) {
            this.machineId = configMachineId;
        } else {
            this.machineId = generateMachineId(this.datacenterId);
        }
    }

    private long generateDatacenterId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                return 1L;
            }
            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return 1L;
            }
            long id = ((0x000000FF & (long) mac[mac.length - 2])
                    | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
            return id % 32;
        } catch (Exception e) {
            return ThreadLocalRandom.current().nextLong(32);
        }
    }

    private long generateMachineId(long datacenterId) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(datacenterId);
            InetAddress ip = InetAddress.getLocalHost();
            if (ip != null) {
                sb.append(ip.getHostAddress());
            }
            sb.append(System.getProperty("user.name"));
            sb.append(System.getProperty("os.name"));
            return (sb.toString().hashCode() & 0xffff) % 32;
        } catch (Exception e) {
            return ThreadLocalRandom.current().nextLong(32);
        }
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨异常");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 4095L;
            if (sequence == 0L) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - 1288834974657L) << 22)
                | (datacenterId << 17)
                | (machineId << 12)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}