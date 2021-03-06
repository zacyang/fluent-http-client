package com.yang.httpClient;

import static com.google.common.collect.Maps.newHashMap;
import static com.yang.httpClient.handler.CommonHandlers.DEFAULT_200_OK_HANDLER;

import java.security.InvalidParameterException;
import java.util.Map;

import com.yang.httpClient.factory.HttpEntityFactory;
import com.yang.httpClient.handler.ExceptionHandler;
import com.yang.httpClient.handler.ExceptionResponseHandler;
import com.yang.httpClient.handler.SuccessResponseHandler;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class HttpClient {
    private String requestUrl;
    private RestTemplate restTemplate;
    private HttpMethod initMethod;
    private Map<HttpStatus, ExceptionResponseHandler> handlers = newHashMap();
    private Map<Class, ExceptionHandler> exceptionHandlers = newHashMap();
    private HttpHeaders headers = new HttpHeaders();
    private Object requestBody;
    private HttpEntityFactory httpEntityFactory;
    private SuccessResponseHandler<Object> successResponseHandler;

    public HttpClient() {
    }

    private HttpClient(RestTemplate restTemplate, HttpMethod httpMethod, String requestUrl) {
        this.successResponseHandler = DEFAULT_200_OK_HANDLER;
        this.initMethod = httpMethod;
        this.requestUrl = requestUrl;
    }

    public HttpClient newRequest(HttpMethod httpMethod, String url) {
        return new HttpClient(new RestTemplate(), httpMethod, url);
    }

    public HttpClient withRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        return this;
    }

    public HttpClient withHeader(String accept, String applicationVndAconexGroupV1Json) {
        headers.set(accept, applicationVndAconexGroupV1Json);
        return this;
    }

    public HttpClient onRespond(HttpStatus httpStatus, ExceptionResponseHandler handler) {
        handlers.put(httpStatus, handler);
        return this;
    }

    public HttpClient onException(Class<? extends Exception> exceptionType, ExceptionHandler handler) {
        exceptionHandlers.put(exceptionType, handler);
        return this;
    }

    public HttpClient onSuccess(SuccessResponseHandler handler) {
        this.successResponseHandler = handler;
        return this;
    }

    public <T> T collectResponse() {
        validate();
        this.restTemplate = this.restTemplate == null ? new RestTemplate() : restTemplate;
        try {
            if (this.httpEntityFactory != null) {
                headers = httpEntityFactory.headers();
            }
            HttpEntity<Object> httpEntity = new HttpEntity<Object>(requestBody, this.headers);

            ResponseEntity<T> result = this.restTemplate.exchange(requestUrl, initMethod, httpEntity, new ParameterizedTypeReference<T>() {
            });

            return this.successResponseHandler.handle(result.getBody());

        } catch (RestClientException e) {
            handleException(e);
        }
        return null;
    }

    private void handleException(RestClientException e) {
        if (e instanceof HttpStatusCodeException) {
            ExceptionResponseHandler exceptionResponseHandler = handlers.get(((HttpStatusCodeException) e).getStatusCode());
            if (exceptionResponseHandler != null) {
                exceptionResponseHandler.handle(((HttpStatusCodeException) e).getResponseHeaders(), ((HttpStatusCodeException) e).getResponseBodyAsByteArray());
            }
        } else {
            Class<? extends Exception> exceptionType = e.getClass();
            ExceptionHandler exceptionHandler = exceptionHandlers.get(exceptionType);
            if (exceptionHandler != null) {
                exceptionHandler.handle(e);
            }
        }
    }

    private void validate() throws InvalidParameterException {
        if (initMethod == null) {
            throw new InvalidParameterException();
        }
    }

    public HttpClient withBody(Object obj) {
        this.requestBody = obj;
        return this;
    }

    public HttpClient withEntityFactory(final HttpEntityFactory httpEntityFactory) {
        this.httpEntityFactory = httpEntityFactory;
        return this;
    }
}
