package com.yang;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

public class GenericHttpClientTest {

    @Mock
    private ResponseHandler successfulHandler;
    @Mock
    private ResponseHandler responseHandler;
    @Mock
    private ExceptionHandler PARAMETER_GENERAL_ERROR_HANDLER;
    @Mock
    private ResponseHandler specificRespCodeHandler;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private ExceptionHandler unexpectedExceptionHandler;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ResponseEntity<String> response;

    private String url = "http://localhost/api";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldSendInitHttpRequestGet() throws Exception {
        initHttpMethodSetUp(HttpMethod.GET, HttpStatus.OK);

        GenericHttpClient.newRequest(HttpMethod.GET, url).
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).execute(String.class);

        verify(successfulHandler).handle();
    }

    @Test
    public void shouldSendInitialHttpMethodPost() throws Exception {
        initHttpMethodSetUp(HttpMethod.POST, HttpStatus.OK);

        GenericHttpClient.newRequest(HttpMethod.POST, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withBody("123").
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).execute(String.class);

        verify(successfulHandler).handle();

    }


    @Test
    public void shouldSendInitialHttpMethodPut() throws Exception {
        initHttpMethodSetUp(HttpMethod.PUT, HttpStatus.OK);

        GenericHttpClient.newRequest(HttpMethod.PUT, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withBody("123").
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).execute(String.class);

        verify(successfulHandler).handle();
    }

    @Test
    public void shouldCallGeneralErrorHandlerWhenAnyError() throws Exception {

        initHttpMethodSetUp(HttpMethod.GET, HttpStatus.NOT_FOUND);

        GenericHttpClient.newRequest(HttpMethod.GET, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withRestTemplate(restTemplate).
                onException(SQLException.class, exceptionHandler).
                onRespond(HttpStatus.NOT_FOUND, specificRespCodeHandler).
                onSuccess(successfulHandler).execute(String.class);

        verifyNoMoreInteractions(successfulHandler);
        verify(specificRespCodeHandler).handle();
    }

    @Test
    public void shouldCallExceptionHandlerWhenExceptionHandlerRegistered() throws Exception {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        ResourceAccessException resourceAccessException = new ResourceAccessException("not able to access resource");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), argument.capture(), any(Class.class))).thenThrow(resourceAccessException);

        GenericHttpClient.newRequest(HttpMethod.GET, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withRestTemplate(restTemplate).
                onException(ResourceAccessException.class, exceptionHandler).
                onRespond(HttpStatus.NOT_FOUND, specificRespCodeHandler).
                onSuccess(successfulHandler).execute(String.class);

        verifyNoMoreInteractions(successfulHandler);
        verifyNoMoreInteractions(specificRespCodeHandler);
        verify(exceptionHandler).handle(resourceAccessException);
    }

    @Test
    public void shouldAbleInitRequestWithSimpleInterface() throws Exception {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), argument.capture(), any(Class.class))).thenReturn(response);


        GenericHttpClient.newRequest(HttpMethod.GET, url).
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).execute(String.class);

        verify(successfulHandler).handle();
    }

    @Test
    public void shouldOnlyNotifyRegisteredHandlerForSpecificException() throws Exception {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        ResourceAccessException actualException = new ResourceAccessException("not able to access resource");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), argument.capture(), any(Class.class))).thenThrow(actualException);

        Class<UnknownHttpStatusCodeException> registeredExceptionHandlerType = UnknownHttpStatusCodeException.class;
        GenericHttpClient.newRequest(HttpMethod.GET, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withRestTemplate(restTemplate).
                onException(registeredExceptionHandlerType, exceptionHandler).
                onRespond(HttpStatus.NOT_FOUND, specificRespCodeHandler).
                onSuccess(successfulHandler).execute(String.class);

        verifyNoMoreInteractions(successfulHandler);
        verifyNoMoreInteractions(specificRespCodeHandler);
        verifyNoMoreInteractions(exceptionHandler);

    }

    private void initHttpMethodSetUp(HttpMethod get, HttpStatus httpStatus) {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(httpStatus);
        when(restTemplate.exchange(anyString(), eq(get), argument.capture(), any(Class.class))).thenReturn(response);
    }
}