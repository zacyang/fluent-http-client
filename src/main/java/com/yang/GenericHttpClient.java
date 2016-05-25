package com.yang;

import static com.google.common.collect.Maps.newHashMap;
import static com.yang.CommonHandlers.DEFAULT_200_OK_HANDLER;
import static com.yang.CommonHandlers.KEY_GENERIC_ERROR_HANDLER;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;

import java.security.InvalidParameterException;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;

public class GenericHttpClient {
    private static final int TOTAL_CONNECTIONS = 60;

    private HttpClient httpClient;
    private HttpMethod initRequest;
    private Map<Integer, ResponseHandler> handlers = newHashMap();
    private Map<Class, ExceptionHandler> exceptionHandlers = newHashMap();

    private GenericHttpClient(HttpMethod httpMethod) {
        this.handlers.put(null, CommonHandlers.DEFAULT_200_OK_HANDLER);
        this.initRequest = httpMethod;
    }

    public static GenericHttpClient newRequest(HttpMethod httpMethod) {
        return new GenericHttpClient(httpMethod);
    }

    public GenericHttpClient withHeader(String accept, String applicationVndAconexGroupV1Json) {
        this.initRequest.setRequestHeader(accept, applicationVndAconexGroupV1Json);
        return this;
    }

    public GenericHttpClient onAnyError(ResponseHandler handler) {
        handlers.put(null, handler);
        return this;
    }


    public GenericHttpClient onRespond(int httpStatus, ResponseHandler handler) {
        handlers.put(httpStatus, handler);
        return this;
    }

    public GenericHttpClient onException(Class<? extends Exception> exceptionTyep, ExceptionHandler handler) {
        exceptionHandlers.put(exceptionTyep, handler);
        return this;
    }

    public GenericHttpClient onSuccess(ResponseHandler handler) {
        handlers.put(SC_OK, handler);
        return this;
    }

    public GenericHttpClient execute(int timeout) throws Exception {
        validate();
        this.httpClient = createOrGetHttpClient(timeout);
        try {
            int returnCode = this.httpClient.executeMethod(initRequest);
            getResponseHandler(returnCode).handle();
        } catch (Exception e) {
            Class<? extends Exception> exceptionType = e.getClass();
            ExceptionHandler exceptionHandler = exceptionHandlers.get(exceptionType);
            exceptionHandler.handle(e);
        }
        return this;
    }

    private void validate() throws InvalidParameterException {
        if (initRequest == null) {
            throw new InvalidParameterException();
        }
    }

    private ResponseHandler getResponseHandler(int returnCode) {
        ResponseHandler responseHandler = handlers.get(returnCode);
        if (null != responseHandler) {
            return responseHandler;
        }
        return getDefaultHandlersByCode(returnCode);
    }

    private ResponseHandler getDefaultHandlersByCode(int returnCode) {
        if (returnCode == SC_OK) {
            return DEFAULT_200_OK_HANDLER;
        } else {
            return handlers.get(KEY_GENERIC_ERROR_HANDLER);
        }
    }

    public GenericHttpClient withRequestEntity(StringRequestEntity stringRequestEntity) {
        if (initRequest instanceof PostMethod) {
            PostMethod postMethod = (PostMethod) this.initRequest;
            postMethod.setRequestEntity(stringRequestEntity);
        } else {
            throw new InvalidParameterException("RequestEntity only applicable for POST method");
        }
        return this;
    }

    private HttpClient createOrGetHttpClient(int timeout) {
        return this.httpClient == null ? createOrGetHttpClient(timeout, timeout, timeout) : httpClient;
    }

    private HttpClient createOrGetHttpClient(int connectionManagerTimeout, int connectionTimeout, int readTimeout) {
        HttpClient httpClient = new HttpClient(createHttpConnectionManager());
        httpClient.getParams().setConnectionManagerTimeout(connectionManagerTimeout);
        httpClient.getParams().setSoTimeout(readTimeout);
        httpClient.getParams().setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, connectionTimeout);
        return httpClient;
    }

    private MultiThreadedHttpConnectionManager createHttpConnectionManager() {
        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setMaxTotalConnections(TOTAL_CONNECTIONS);
        params.setDefaultMaxConnectionsPerHost(TOTAL_CONNECTIONS);
        multiThreadedHttpConnectionManager.setParams(params);
        return multiThreadedHttpConnectionManager;
    }


    public GenericHttpClient withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

}
