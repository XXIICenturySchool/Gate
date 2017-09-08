package com.example.gate;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.xml.ws.Service;
import java.util.Date;
import java.util.List;
import java.util.Random;

public enum Services {
    DISCOVERY("discovery"),
    MAPLOGIN("maplogin"),
    MAP("maplogin"),
    LOGIN("maplogin"),
    MATH("mathservice"),
    TELEGRAM("telegram"),
    AMERICAN_QUESTIONS("american-questions"),
    GEOMETRY("geometry");

    public final String id;

    Services(String id) {
        this.id = id;
    }

    public static Services getServiceById(String id){
        for (Services s : Services.values()) {
            if(s.id.equalsIgnoreCase(id)) return  s;
        }
        return null;
    }

    private static final Random random = new Random(new Date().getTime());
    private static ServiceInstance pickRandomInstanceFromList(List<ServiceInstance> instances) {
        return instances.get(Math.abs(random.nextInt(instances.size()) % instances.size()));
    }

    public ServiceInstance pickRandomInstance(DiscoveryClient discoveryClient) {
        return pickRandomInstanceFromList(discoveryClient.getInstances(id));
    }
}