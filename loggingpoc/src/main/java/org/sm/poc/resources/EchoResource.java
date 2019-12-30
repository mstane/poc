package org.sm.poc.resources;

import com.codahale.metrics.annotation.Timed;
import org.sm.poc.api.LoggingPocBean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Path("/echo")
@Produces(MediaType.APPLICATION_JSON)
public class EchoResource {

    private final String template;
    private final String defaultName;
    private final AtomicLong counter;

    public EchoResource(String template, String defaultName) {
        this.template = template;
        this.defaultName = defaultName;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed
    public LoggingPocBean echo(@QueryParam("name") Optional<String> name) {
        final String value = String.format(template, name.orElse(defaultName));
        return new LoggingPocBean(counter.incrementAndGet(), value);
    }

}