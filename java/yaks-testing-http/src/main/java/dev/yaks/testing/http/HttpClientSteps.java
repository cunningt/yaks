package dev.yaks.testing.http;

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.builder.BuilderSupport;
import com.consol.citrus.dsl.builder.HttpActionBuilder;
import com.consol.citrus.dsl.builder.HttpClientActionBuilder;
import com.consol.citrus.dsl.builder.HttpClientRequestActionBuilder;
import com.consol.citrus.dsl.builder.HttpClientResponseActionBuilder;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.http.message.HttpMessage;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 */
public class HttpClientSteps {

    @CitrusResource
    private TestRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private HttpClient httpClient;

    private String requestUrl;

    private HttpMessage request;
    private HttpMessage response;

    private Map<String, String> requestHeaders = new HashMap<>();
    private Map<String, String> responseHeaders = new HashMap<>();

    private Map<String, String> bodyValidationExpressions = new HashMap<>();

    private String requestBody;
    private String responseBody;

    @Before
    public void before(Scenario scenario) {
        if (httpClient == null && citrus.getApplicationContext().getBeansOfType(HttpClient.class).size() == 1L) {
            httpClient = citrus.getApplicationContext().getBean(HttpClient.class);
        } else {
            httpClient = CitrusEndpoints.http()
                    .client()
                    .build();
        }

        requestHeaders = new HashMap<>();
        responseHeaders = new HashMap<>();
        request = new HttpMessage();
        response = new HttpMessage();
        requestBody = null;
        responseBody = null;
        bodyValidationExpressions = new HashMap<>();
    }

    @Given("^http-client \"([^\"\\s]+)\"$")
    public void setClient(String id) {
        if (!citrus.getApplicationContext().containsBean(id)) {
            throw new CitrusRuntimeException("Unable to find http client for id: " + id);
        }

        httpClient = citrus.getApplicationContext().getBean(id, HttpClient.class);
    }

    @Given("^(?:URL|url): ([^\\s]+)$")
    public void setUrl(String url) {
        try {
            URL requestURL = new URL(url);
            if (requestURL.getProtocol().equalsIgnoreCase("https")) {
                httpClient.getEndpointConfiguration().setRequestFactory(sslRequestFactory());
            }

            this.requestUrl = url;
        } catch (MalformedURLException e) {
            throw new CitrusRuntimeException(e);
        }
    }

    @Then("^(?:expect|verify) HTTP response header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addResponseHeader(String name, String value) {
        responseHeaders.put(name, value);
    }

    @Then("^(?:expect|verify) HTTP response headers$")
    public void addResponseHeaders(DataTable headers) {
        Map<String, String> headerPairs = headers.asMap(String.class, String.class);
        headerPairs.forEach(this::addResponseHeader);
    }

    @Given("^HTTP request header ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addRequestHeader(String name, String value) {
        requestHeaders.put(name, value);
    }

    @Given("^HTTP request headers$")
    public void addRequestHeaders(DataTable headers) {
        Map<String, String> headerPairs = headers.asMap(String.class, String.class);
        headerPairs.forEach(this::addRequestHeader);
    }

    @Then("^(?:expect|verify) HTTP response expression: ([^\\s]+)(?:=| is )\"(.+)\"$")
    public void addBodyValidationExpression(String name, String value) {
        bodyValidationExpressions.put(name, value);
    }

    @Then("^(?:expect|verify) HTTP response expressions$")
    public void addBodyValidationExpressions(DataTable validationExpressions) {
        Map<String, String> expressions = validationExpressions.asMap(String.class, String.class);
        expressions.forEach(this::addBodyValidationExpression);
    }

    @Given("^HTTP request body$")
    public void setRequestBodyMultiline(String body) {
        setRequestBody(body);
    }

    @Given("^HTTP request body: (.+)$")
    public void setRequestBody(String body) {
        this.requestBody = body;
    }

    @Then("^(?:expect|verify) HTTP response body$")
    public void setResponseBodyMultiline(String body) {
        setResponseBody(body);
    }

    @Then("^(?:expect|verify) HTTP response body: (.+)$")
    public void setResponseBody(String body) {
        this.responseBody = body;
    }

    @When("^send HTTP request$")
    public void sendClientRequestFull(String requestData) {
        sendClientRequest(HttpMessage.fromRequestData(requestData));
    }

    @Then("^receive HTTP response$")
    public void receiveClientResponseFull(String responseData) {
        receiveClientResponse(HttpMessage.fromResponseData(responseData));
    }

    @When("^send (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE)$")
    public void sendClientRequestMultilineBody(String method) {
        sendClientRequest(method, null);
    }

    @When("^send (GET|HEAD|POST|PUT|PATCH|DELETE|OPTIONS|TRACE) ([^\"\\s]+)$")
    public void sendClientRequest(String method, String path) {
        request.method(HttpMethod.valueOf(method));

        if (StringUtils.hasText(path)) {
            request.path(path);
            request.contextPath(path);
        }

        if (StringUtils.hasText(requestBody)) {
            request.setPayload(requestBody);
        }

        for (Map.Entry<String, String> headerEntry : requestHeaders.entrySet()) {
            request.setHeader(headerEntry.getKey(), headerEntry.getValue());
        }

        sendClientRequest(request);

        requestBody = null;
        requestHeaders.clear();
    }

    @Then("^receive HTTP (\\d+)(?: [^\\s]+)?$")
    public void receiveClientResponse(Integer status) {
        response.status(HttpStatus.valueOf(status));

        if (StringUtils.hasText(responseBody)) {
            response.setPayload(responseBody);
        }

        for (Map.Entry<String, String> headerEntry : responseHeaders.entrySet()) {
            response.setHeader(headerEntry.getKey(), headerEntry.getValue());
        }

        receiveClientResponse(response);

        responseBody = null;
        responseHeaders.clear();
    }

    /**
     * Sends client request.
     * @param request
     */
    private void sendClientRequest(HttpMessage request) {
        BuilderSupport<HttpActionBuilder> action = builder -> {
            HttpClientActionBuilder.HttpClientSendActionBuilder sendBuilder = builder.client(httpClient).send();
            HttpClientRequestActionBuilder requestBuilder;

            if (request.getRequestMethod() == null || request.getRequestMethod().equals(HttpMethod.POST)) {
                requestBuilder = sendBuilder.post().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.GET)) {
                requestBuilder = sendBuilder.get().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.PUT)) {
                requestBuilder = sendBuilder.put().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.DELETE)) {
                requestBuilder = sendBuilder.delete().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.HEAD)) {
                requestBuilder = sendBuilder.head().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.TRACE)) {
                requestBuilder = sendBuilder.trace().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.PATCH)) {
                requestBuilder = sendBuilder.patch().message(request);
            } else if (request.getRequestMethod().equals(HttpMethod.OPTIONS)) {
                requestBuilder = sendBuilder.options().message(request);
            } else {
                requestBuilder = sendBuilder.post().message(request);
            }

            if (StringUtils.hasText(requestUrl)) {
                requestBuilder.uri(requestUrl);
            }
        };

        runner.http(action);
    }

    /**
     * Receives client response.
     * @param response
     */
    private void receiveClientResponse(HttpMessage response) {
        runner.http(action -> {
            HttpClientResponseActionBuilder responseBuilder = action.client(httpClient).receive()
                    .response(response.getStatusCode())
                    .message(response);

            for (Map.Entry<String, String> headerEntry : bodyValidationExpressions.entrySet()) {
                responseBuilder.validate(headerEntry.getKey(), headerEntry.getValue());
            }
            bodyValidationExpressions.clear();
        });
    }

    /**
     * Get secure request factory.
     * @return
     */
    private HttpComponentsClientHttpRequestFactory sslRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(sslClient());
    }

    /**
     * Get secure http client implementation with trust all strategy and noop host name verifier.
     * @return
     */
    private org.apache.http.client.HttpClient sslClient() {
        try {
            SSLContext sslcontext = SSLContexts
                    .custom()
                    .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                    sslcontext, NoopHostnameVerifier.INSTANCE);

            return HttpClients
                    .custom()
                    .setSSLSocketFactory(sslSocketFactory)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new CitrusRuntimeException("Failed to create http client for ssl connection", e);
        }
    }
}
