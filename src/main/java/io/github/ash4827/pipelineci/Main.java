package io.github.ash4827.pipelineci;

public class Main {
    public static void main(String[] args) {
        var client = io.github.ash4827.pipelineci.runner.DockerClientFactory.create();
        System.out.println("Connected to Docker!");
    }
}