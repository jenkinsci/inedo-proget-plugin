package com.inedo.utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.inedo.proget.api.ProGetConfig;

/**
 * A Mocked server that replaces a live ProGet installation
 * 
 * @author Andrew Sumner
 */
public class MockServer {
    private HttpServer server = null;
    private HttpRequestHandler handler;
    private ProGetConfig config;

    public MockServer() throws IOException {
        handler = new HttpHandler();
        server = ServerBootstrap.bootstrap()
                .setLocalAddress(InetAddress.getLocalHost())
                .setListenerPort(0) // Any free port
                .registerHandler("*", handler)
                .create();

        server.start();

        config = new ProGetConfig();
        config.url = "http://" + server.getInetAddress().getHostName() + ":" + server.getLocalPort();
        config.apiKey = "1";
        config.user = "User";
        config.password = "Password";
    }

    public ProGetConfig getProGetConfig() {
        return config;
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    // Handler for the test server that returns responses based on the requests.
    public class HttpHandler implements HttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
            URI uri = URI.create(request.getRequestLine().getUri());

            String method = uri.getPath().replace("/api/json/", "");

            switch (method) {
            case "Feeds_GetFeeds":
                response.setEntity(MockData.FEEDS.getInputSteam());
                break;

            case "Feeds_GetFeed":
                response.setEntity(MockData.FEED.getInputSteam());
                break;

            case "ProGetPackages_GetPackages":
                response.setEntity(MockData.PACKAGES.getInputSteam());
                break;

            case "ProGetPackages_GetPackageVersions":
                response.setEntity(MockData.PACKAGE_VERSIONS.getInputSteam());
                break;

            case "/upack/Example/upload":
                response.setStatusCode(HttpStatus.SC_OK);
                break;

            case "/upack/Example/download/andrew/sumner/proget/ExamplePackage": // Latest version
            case "/upack/Default/download/andrew/sumner/proget/ExamplePackage/0.0.1": // Specific version
            case "/upack/Example/download/andrew/sumner/proget/ExamplePackage/0.0.3": // Plugin Test
                try {
                    File file = new File("src/test/resources/com/inedo/proget/api/example-0.0.1.upack");
                    FileEntity body = new FileEntity(file, ContentType.DEFAULT_BINARY);
                    response.setHeader("Content-Type", "application/force-download");
                    response.setHeader("Content-Disposition", "attachment; filename=\"example-0.0.1.upack\"");
                    response.setEntity(body);
                } catch (Exception e) {
                    System.err.println(e);
                }

                break;

            case "/api/version":
                response.setEntity(new StringEntity("ProGet vs ?"));
                break;
                
            default:
                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                response.setEntity(new StringEntity("API method " + method + " not found."));
            }
        }
    }
}
