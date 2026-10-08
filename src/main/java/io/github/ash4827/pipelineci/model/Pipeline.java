package io.github.ash4827.pipelineci.model;
import java.util.List;
import java.util.Map;

public record Pipeline(String name, List<Step> steps, Map<String, String> env) {

    public Pipeline {
        env = (env == null) ? Map.of() : Map.copyOf(env);

        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Pipeline must have at least one step");
        }

        steps = List.copyOf(steps);
    }
}
