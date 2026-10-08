package io.github.ash4827.pipelineci.model;

import java.time.Duration;
import java.util.Map;

public record Step(
        String name,
        String image,
        String run,
        Map<String, String> env,
        Duration timeout) {

    public Step {
        env = (env == null) ? Map.of() : Map.copyOf(env);
        timeout = (timeout == null) ? Duration.ofMinutes(5) : timeout;
    }
}