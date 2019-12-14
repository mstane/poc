package org.sm.poc.loggingpoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/echo")
public class EchoController {

    private final Logger log = LoggerFactory.getLogger(EchoController.class);

    private static final String template = "LoggingPoc:echo text[%d]: [%s]";

    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/{text}")
    public LoggingPocBean log(@PathVariable("text") String text) {
        return execute(text);
    }

    @GetMapping("/")
    public LoggingPocBean log() {
        return execute("{Empty text}");
    }

    private LoggingPocBean execute(String text) {
        long id = counter.incrementAndGet();
        String loggedText = String.format(template, id, text);

        log.info(loggedText);

        return new LoggingPocBean(id, loggedText);
    }


}
