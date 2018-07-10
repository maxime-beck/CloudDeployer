package org.apache.tomcat.cloud.model;

public class Deployment {
    private String name;
    private String port;
    private DeploymentMode mode;
    private String warPath;
    private String exposedPort;
    private int replicas;
    private String registryId;
    private String repositoryName;

    private Docker docker;
    private Provider provider;

    public static enum DeploymentMode {
        MONOLITHIC,
        MICROSERVICE;
    }

    public Deployment(String name, String port, DeploymentMode mode,
    String warPath, String exposedPort, int replicas,
    String registryId, String repositoryName,
    Docker docker, Provider provider) {
        this.name = name;
        this.port = port;
        this.mode = mode;
        this.warPath = warPath;
        this.exposedPort = exposedPort;
        this.replicas = replicas;
        this.registryId = registryId;
        this.repositoryName = repositoryName;
        this.docker = docker;
        this.provider = provider;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public DeploymentMode getMode() {
        return mode;
    }

    public void setMode(DeploymentMode mode) {
        this.mode = mode;
    }

    public String getWarPath() {
        return warPath;
    }

    public void setWarPath(String warPath) {
        this.warPath = warPath;
    }

    public String getExposedPort() {
        return exposedPort;
    }

    public void setExposedPort(String exposedPort) {
        this.exposedPort = exposedPort;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public Docker getDocker() {
        return this.docker;
    }

    public void setDocker(Docker docker) {
        this.docker = docker;
    }

    public Provider getProvider() {
        return this.provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getRegistryId() {
        return registryId;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
}
