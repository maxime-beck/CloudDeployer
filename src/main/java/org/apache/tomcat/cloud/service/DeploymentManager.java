package org.apache.tomcat.cloud.service;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.cloud.model.Deployment;
import org.apache.tomcat.cloud.model.Provider;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.awt.Desktop;
import java.net.URI;

public class DeploymentManager {

    private final String DOCKER_AUTH_JSON_KEY_USER = "_json_key";
    private final String DEPLOY_PATH = "/deployment.json";
    private final String EXPOSE_PATH = "/expose.json";
    private final String ROUTE_PATH = "/route.json";


    private Deployment deployment;
    private CloseableHttpClient httpclient;

    private DockerClient dockerClient;
    private AuthConfig dockerAuth;
    private String dockerImageTag;

    private String deployUrl;
    private String exposeUrl;
    private String routeUrl;

    public DeploymentManager(Deployment deployment) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this.deployment = deployment;

        this.deployUrl = "https://" + deployment.getProvider().getHostAddress() + "/apis/extensions/v1beta1/namespaces/" + deployment.getRegistryId() + "/deployments";
        this.exposeUrl = "https://" + deployment.getProvider().getHostAddress() + "/api/v1/namespaces/" + deployment.getRegistryId() + "/services";
        this.routeUrl = "https://" + deployment.getProvider().getHostAddress() + "/oapi/v1/namespaces/" + deployment.getRegistryId() + "/routes";
        init();
    }

    public void deploy(Deployment deployment, BuildImageResultCallback buildCallback,
                       PushImageResultCallback pushCallback) throws IOException {
        URL microDockerFileURL = MyClass.class.getClassLoader().getResource(resourceName);
        URL monoDockerFileURL = MyClass.class.getClassLoader().getResource(resourceName);

        if(deployment.getMode().equals(Deployment.DeploymentMode.MONOLITHIC))
            dockerBuild(monoDockerFileURL.getPath(), dockerImageTag, buildCallback);
        else if (deployment.getMode().equals(Deployment.DeploymentMode.MICROSERVICE))
            dockerBuild(microDockerFileURL.getPath(), dockerImageTag, buildCallback);
        else {
            // TODO Implement logs and exception handlers
            System.out.println("Error : Mode property must be set to either MONOLITHIC or MICROSERVICE.");
        }

        dockerPush(dockerImageTag, pushCallback);
        deploy(DEPLOY_PATH);
        expose(EXPOSE_PATH);

        if(deployment.getProvider().getType().equals(Provider.ProviderType.OPENSHIFT))
            createRoute(ROUTE_PATH);
    }

    private void init() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, IOException {
        // TODO Use appropriate certificate
        /* Solution by accepting all certificate (insecure) */
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        this.httpclient = HttpClients.custom().setSSLContext(ctx).setSSLHostnameVerifier((new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        })).build();

        /* Docker init */
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        dockerClient = DockerClientBuilder.getInstance(config).build();
        dockerAuth = null;

        if(deployment.getDocker().getAuthFile() != null)
            dockerAuth = dockerAuthconfig(deployment.getDocker().getAuthFile());
        else if (deployment.getDocker().getUsername() != null && deployment.getDocker().getToken() != null)
            dockerAuth = dockerAuthconfig(deployment.getDocker().getUsername(), deployment.getDocker().getToken());

        //TODO add support for Azure
        switch (deployment.getProvider().getType()) {
            case AZURE:
            case GCLOUD:
            case OPENSHIFT:
                dockerImageTag = deployment.getDocker().getRegistryAddress() + "/" +
                                 deployment.getRegistryId() + "/" +
                                 deployment.getDocker().getImageName() + ":" + deployment.getDocker().getImageVersion();
                break;
            case AWS:
                dockerImageTag = deployment.getRegistryId() + "." +
                                 deployment.getDocker().getRegistryAddress() + "/" +
                                 deployment.getRepositoryName() + ":" + deployment.getDocker().getImageVersion();
                break;
        }
    }

    private AuthConfig dockerAuthconfig(String authFile) throws IOException {
        return dockerAuthconfig(DOCKER_AUTH_JSON_KEY_USER, readFile(authFile, StandardCharsets.UTF_8));
    }

    private AuthConfig dockerAuthconfig(String username, String password) throws IOException {
        return new AuthConfig()
                .withUsername(username)
                .withPassword(password)
                .withRegistryAddress("https://" + deployment.getDocker().getRegistryAddress());
    }

    private void deploy(String specFile) throws IOException {
        System.out.println("Deploying...");
        String specs = readFile(specFile, StandardCharsets.UTF_8);
        specs = specs.replace("$NAME" , deployment.getName());
        specs = specs.replace("$IMAGE" , dockerImageTag);
        specs = specs.replace("$PORT" , deployment.getPort());
        specs = specs.replace("$REPLICAS" , new Integer(deployment.getReplicas()).toString());
        specs = specs.replace("$SECRET" , deployment.getDocker().getRegistrySecret());
        sslRequestPOST(deployUrl, specs);
    }

    private void expose(String specFile) throws IOException {
        System.out.println("Exposing...");
        String specs = readFile(specFile, StandardCharsets.UTF_8);
        specs = specs.replace("$NAME" , deployment.getName());
        specs = specs.replace("$DEPLOYED_PORT" , deployment.getPort());
        specs = specs.replace("$EXPOSED_PORT" , deployment.getExposedPort());
        sslRequestPOST(exposeUrl, specs);
    }

    private void createRoute(String specFile) throws IOException {
        System.out.println("Creating route...");
        String specs = readFile(specFile, StandardCharsets.UTF_8);
        String host = deployment.getName() + "-" + deployment.getRegistryId() + "." + (deployment.getProvider().getHostAddress().split(":"))[0] + ".nip.io";
        specs = specs.replace("$SERVICE_NAME" , deployment.getName());
        specs = specs.replace("$NAMESPACE" , deployment.getRegistryId());
        specs = specs.replace("$HOST" , host);
        sslRequestPOST(routeUrl, specs);
    }

    private CloseableHttpResponse sslRequestPOST(String url, String jsonBody) throws IOException {
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        request.addHeader("Authorization", "Bearer " + deployment.getDocker().getToken());
        StringEntity entity_json = new StringEntity(jsonBody);
        request.setEntity(entity_json);
        return httpclient.execute(request);
    }

    private CloseableHttpResponse sslRequestGET(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        return httpclient.execute(request);
    }

    private void dockerBuild(String dockerfileBaseDir, String tag, BuildImageResultCallback callback) {
        File dockerFile = new File(dockerfileBaseDir);

        dockerClient.buildImageCmd(dockerFile)
                .withBuildArg("registry_id", deployment.getRegistryId())
                .withBuildArg("war", deployment.getWarPath())
                .withTags(new HashSet<String>(Arrays.asList(tag)))
                .exec(callback).awaitImageId();
    }

    private void dockerPush(String tag, PushImageResultCallback callback) {
        if(dockerAuth != null)
            dockerClient.pushImageCmd(tag)
                    .withAuthConfig(dockerAuth)
                    .exec(callback).awaitSuccess();
        else
            dockerClient.pushImageCmd(tag)
                    .exec(callback).awaitSuccess();
    }

    private String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
