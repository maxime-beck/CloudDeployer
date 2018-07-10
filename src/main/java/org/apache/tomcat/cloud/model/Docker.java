package org.apache.tomcat.cloud.model;

public class Docker {
    private String username;
    private String token;
    private String authFile;
    private String registryAddress;
    private String registrySecret;
    private String imageName;
    private String imageVersion;

    public Docker(String username,String token, String authFile,
                      String registryAddress, String registrySecret,
                      String imageName, String imageVersion) {
        this.username = username;
        this.token = token;
        this.authFile = authFile;
        this.registryAddress = registryAddress;
        this.registrySecret = registrySecret;
        this.imageName = imageName;
        this.imageVersion = imageVersion;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAuthFile() {
        return authFile;
    }

    public void setAuthFile(String authFile) {
        this.authFile = authFile;
    }

    public String getRegistryAddress() {
        return registryAddress;
    }

    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public String getRegistrySecret() {
        return registrySecret;
    }

    public void setRegistrySecret(String registrySecret) {
        this.registrySecret = registrySecret;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageVersion() {
        return imageVersion;
    }

    public void setImageVersion(String imageVersion) {
        this.imageVersion = imageVersion;
    }

}
