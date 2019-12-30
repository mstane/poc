package org.sm.poc.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.lang3.StringUtils;
import org.sm.poc.api.LoggingPocBean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

@Path("/env")
@Produces(MediaType.APPLICATION_JSON)
public class EnvResource {

    private final AtomicLong counter;

    public EnvResource() {
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    public LoggingPocBean echo() throws IOException {
        long currentValue = counter.incrementAndGet();
        String pathString = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        String output = null;
        if (!StringUtils.isEmpty(pathString)) {
            output = Files.readString(Paths.get(pathString));
        }
        output += printOutMetadata();
        output += printOutSystemProperties();
        output += printOutHostData();
        return new LoggingPocBean(currentValue, String.format("GOOGLE_APPLICATION_CREDENTIALS=[%s]; content=[%s]; ", pathString, output));
    }

    private String printOutMetadata() {
        String instanceId = com.google.cloud.MetadataConfig.getInstanceId();
        String namespaceId = com.google.cloud.MetadataConfig.getNamespaceId();
        String projectId = com.google.cloud.MetadataConfig.getProjectId();
        String clusterName = com.google.cloud.MetadataConfig.getClusterName();
        String containerName = com.google.cloud.MetadataConfig.getContainerName();
        String zone = com.google.cloud.MetadataConfig.getZone();
        return String.format("instanceId:%s; namespaceId:%s; projectId:%s; clusterName:%s; containerName:%s; zone:%s",
                instanceId, namespaceId, projectId, clusterName, containerName, zone);
    }

    private String printOutSystemProperties() {
        StringBuilder sb = new StringBuilder();
        Properties properties = System.getProperties();
        properties.forEach((k, v) -> sb.append(k + ": " + v + "; "));
        return sb.toString();
    }

    private String printOutHostData() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return String.format("----- address = %s; hostname = %s -------", inetAddress.getHostAddress(), inetAddress.getHostName());
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

}