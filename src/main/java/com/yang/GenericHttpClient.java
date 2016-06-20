package com.yang;

import static com.google.common.collect.Maps.newHashMap;

import java.security.InvalidParameterException;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class GenericHttpClient {
    private static String url;

    private RestTemplate restTemplate;
    private HttpMethod initMethod;
    private Map<HttpStatus, ResponseHandler> handlers = newHashMap();
    private Map<Class, ExceptionHandler> exceptionHandlers = newHashMap();
    private HttpHeaders headers = new HttpHeaders();
    private Object requestBody;

    private GenericHttpClient(RestTemplate restTemplate, HttpMethod httpMethod, String url) {
        this.handlers.put(null, CommonHandlers.DEFAULT_200_OK_HANDLER);
        this.initMethod = httpMethod;
        this.url = url;
    }

    public static GenericHttpClient newRequest(HttpMethod httpMethod, String url) {
        return new GenericHttpClient(new RestTemplate(), httpMethod, url);
    }
    public  GenericHttpClient withRestTemplate(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        return this;
    }

    public GenericHttpClient withHeader(String accept, String applicationVndAconexGroupV1Json) {
        headers.set(accept, applicationVndAconexGroupV1Json);
        return this;
    }

    public GenericHttpClient onRespond(HttpStatus httpStatus, ResponseHandler handler) {
        handlers.put(httpStatus, handler);
        return this;
    }

    public GenericHttpClient onException(Class<? extends Exception> exceptionType, ExceptionHandler handler) {
        exceptionHandlers.put(exceptionType, handler);
        return this;
    }

    public GenericHttpClient onSuccess(ResponseHandler handler) {
        handlers.put(HttpStatus.OK, handler);
        return this;
    }

    public GenericHttpClient execute(Class responseClass) throws Exception {
        validate();
        this.restTemplate = this.restTemplate == null? new RestTemplate() : restTemplate;
        try {
            HttpEntity<Object> httpEntity = new HttpEntity<Object>(requestBody, this.headers);
            ResponseEntity result = this.restTemplate.exchange(url, initMethod, httpEntity, responseClass);
            //get result here?
            result.getBody();
            getResponseHandler(result.getStatusCode()).handle();

        } catch (RestClientException e) {

            if (e instanceof HttpStatusCodeException) {
                ResponseHandler responseHandler = handlers.get(((HttpStatusCodeException) e).getStatusCode());
                if(responseHandler != null){
                    responseHandler.handle();
                }
            } else {
                Class<? extends Exception> exceptionType = e.getClass();
                ExceptionHandler exceptionHandler = exceptionHandlers.get(exceptionType);
                if(exceptionHandler != null){
                    exceptionHandler.handle(e);
                }
            }
        }
        return this;
    }

    private void validate() throws InvalidParameterException {
        if (initMethod == null) {
            throw new InvalidParameterException();
        }
    }

    private ResponseHandler getResponseHandler(HttpStatus returnCode) {
        ResponseHandler responseHandler = handlers.get(returnCode);
        if (null != responseHandler) {
            return responseHandler;
        }
        return getDefaultHandlersByCode(returnCode);
    }

    private ResponseHandler getDefaultHandlersByCode(HttpStatus returnCode) {
        if (returnCode == HttpStatus.OK) {
            return CommonHandlers.DEFAULT_200_OK_HANDLER;
        } else {
            return handlers.get(null);
        }
    }

    public GenericHttpClient withBody(Object obj) {
        this.requestBody = obj;
        return this;
    }
}
