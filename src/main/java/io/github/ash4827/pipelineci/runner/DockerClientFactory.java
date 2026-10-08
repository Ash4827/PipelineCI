package io.github.ash4827.pipelineci.runner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.time.Duration;

public class DockerClientFactory {

    public static DockerClient create() {
        DefaultDockerClientConfig config =
                DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        DockerHttpClient http = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(5))
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        DockerClient client = DockerClientImpl.getInstance(config, http);

        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot reach Docker at " + config.getDockerHost()
                    + ". Is Docker Desktop running?", e);
        }
        return client;
    }
}
