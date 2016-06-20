package com.yang;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import com.yang.httpClient.HttpClient;
import com.yang.httpClient.factory.HttpEntityFactory;
import com.yang.httpClient.handler.ExceptionHandler;
import com.yang.httpClient.handler.ExceptionResponseHandler;
import com.yang.httpClient.handler.SuccessResponseHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

public class HttpClientTest {

    @Mock
    private SuccessResponseHandler successfulHandler;
    @Mock
    private ExceptionResponseHandler exceptionResponseHandler;
    @Mock
    private ExceptionResponseHandler specificRespCodeHandler;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private ExceptionHandler unexpectedExceptionHandler;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private HttpEntityFactory httpEntityFactory;
    @Mock
    private ResponseEntity<String> response;

    private HttpClient testSubject;
    private String url = "http://localhost/api";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testSubject = new HttpClient();
    }

    @Test
    public void shouldSendInitHttpRequestGet() throws Exception {
        initHttpMethodSetUp(HttpMethod.GET, HttpStatus.OK);

        when(successfulHandler.handle(anyString())).thenReturn("OK");
        String result = testSubject.newRequest(HttpMethod.GET, url).
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).collectResponse();

        verify(successfulHandler).handle("OK");
        assertThat(result, is("OK"));
    }

    @Test
    public void shouldSendInitialHttpMethodPost() throws Exception {
        initHttpMethodSetUp(HttpMethod.POST, HttpStatus.OK);

        String result = testSubject.newRequest(HttpMethod.POST, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withBody("123").
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).collectResponse();
        when(successfulHandler.handle(result)).thenReturn(result);

        verify(successfulHandler).handle("OK");
    }


    @Test
    public void shouldSendInitialHttpMethodPut() throws Exception {
        initHttpMethodSetUp(HttpMethod.PUT, HttpStatus.OK);

        when(successfulHandler.handle("OK")).thenReturn("OK");
        String result = testSubject.newRequest(HttpMethod.PUT, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withBody("123").
                withRestTemplate(restTemplate).
                onSuccess(successfulHandler).collectResponse();


        verify(successfulHandler).handle(result);
        assertThat(result, is("OK"));
    }

    @Test
    public void shouldCallExceptionHandlerWhenExceptionHandlerRegistered() throws Exception {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        ResourceAccessException resourceAccessException = new ResourceAccessException("not able to access resource");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), argument.capture(), any(ParameterizedTypeReference.class))).thenThrow(resourceAccessException);

        testSubject.newRequest(HttpMethod.GET, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withRestTemplate(restTemplate).
                onException(ResourceAccessException.class, exceptionHandler).
                onRespond(HttpStatus.NOT_FOUND, specificRespCodeHandler).
                onSuccess(successfulHandler).collectResponse();

        verifyNoMoreInteractions(successfulHandler);
        verifyNoMoreInteractions(specificRespCodeHandler);
        verify(exceptionHandler).handle(resourceAccessException);
    }

    @Test
    public void shouldOnlyNotifyRegisteredHandlerForSpecificException() throws Exception {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        ResourceAccessException actualException = new ResourceAccessException("not able to access resource");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), argument.capture(), any(ParameterizedTypeReference.class))).thenThrow(actualException);

        Class<UnknownHttpStatusCodeException> registeredExceptionHandlerType = UnknownHttpStatusCodeException.class;
        String result = testSubject.newRequest(HttpMethod.GET, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withRestTemplate(restTemplate).
                onException(registeredExceptionHandlerType, exceptionHandler).
                onRespond(HttpStatus.NOT_FOUND, specificRespCodeHandler).
                onSuccess(successfulHandler).collectResponse();

        verifyNoMoreInteractions(successfulHandler);
        verifyNoMoreInteractions(specificRespCodeHandler);
        verifyNoMoreInteractions(exceptionHandler);
    }

    @Test
    public void shouldDelegateToHttpEntityFactoryToGenerateHeader() throws Exception {
        String ok = "OK";
        HttpMethod get = HttpMethod.GET;

        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn(ok);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(get), argument.capture(), any(ParameterizedTypeReference.class))).thenReturn(response);

        when(successfulHandler.handle(ok)).thenReturn(ok);
        HttpHeaders generatedHeaders = new HttpHeaders();
        generatedHeaders.set(HttpHeaders.ACCEPT, "application/question");
        when(httpEntityFactory.headers()).thenReturn(generatedHeaders);

        String result = testSubject.newRequest(get, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withBody("123").
                withRestTemplate(restTemplate).
                withEntityFactory(httpEntityFactory).
                onSuccess(successfulHandler).collectResponse();

        List<String> strings = argument.getValue().getHeaders().get(HttpHeaders.ACCEPT);
        assertThat(strings.contains("application/question"), is(true));
        verify(successfulHandler).handle(result);
        assertThat(result, is(ok));

    }

    private void initHttpMethodSetUp(HttpMethod get, HttpStatus httpStatus) {
        ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
        when(response.getBody()).thenReturn("OK");
        when(response.getStatusCode()).thenReturn(httpStatus);
        when(restTemplate.exchange(anyString(), eq(get), argument.capture(), any(ParameterizedTypeReference.class))).thenReturn(response);
    }
}