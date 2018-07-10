package org.apache.tomcat.cloud.model;

public class Provider {
    private ProviderType type;
    private String hostAddress;

    public enum ProviderType {
        GCLOUD,
        AWS,
        AZURE,
        OPENSHIFT;
    }

    public Provider(ProviderType type, String hostAddress) {
        this.type = type;
        this.hostAddress = hostAddress;
    }

    public ProviderType getType() {
        return type;
    }

    public void getType(ProviderType type) {
        this.type = type;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

}
