package com.yang;

import static com.yang.GenericHttpClient.newRequest;
import static org.apache.commons.httpclient.HttpStatus.SC_NOT_FOUND;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.sql.SQLException;

import com.google.common.net.HttpHeaders;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class GenericHttpClientTest {

    @Mock
    private HttpClient httpClient;
    @Mock
    private PostMethod post;
    @Mock
    private GetMethod get;
    @Mock
    private PutMethod put;
    @Mock
    private ResponseHandler SUCCESSFUL_HANDLER;
    @Mock
    private ResponseHandler GENERAL_ERROR_HANDLER;
    @Mock
    private ExceptionHandler PARAMETER_GENERAL_ERROR_HANDLER;
    @Mock
    private ResponseHandler SPECIFIC_ERROR_HANDLER;
    @Mock
    private StringRequestEntity stringEntity;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private ExceptionHandler unexpectedExceptionHandler;


    @Before
    public void setUp() throws Exception {
        initMocks(this);

    }

    @Test
    public void shouldSendInitialHttpMethodPost() throws Exception {
        when(httpClient.executeMethod(post)).thenReturn(HttpStatus.SC_OK);

        newRequest(post)
                .withHttpClient(httpClient)
                .execute(10);

        verify(httpClient).executeMethod(post);
    }

    @Test
    public void shouldSendInitialHttpMethodGet() throws Exception {
        when(httpClient.executeMethod(get)).thenReturn(HttpStatus.SC_OK);

        newRequest(get)
                .withHttpClient(httpClient)
                .execute(10);

        verify(httpClient).executeMethod(get);
    }

    @Test
    public void shouldSendInitialHttpMethodPut() throws Exception {
        when(httpClient.executeMethod(put)).thenReturn(HttpStatus.SC_OK);

        newRequest(put)
                .withHttpClient(httpClient)
                .execute(10);

        verify(httpClient).executeMethod(put);

    }

    @Test
    public void shouldCallSuccessHandlerWhenSuccuessed() throws Exception {
        when(httpClient.executeMethod(get)).thenReturn(HttpStatus.SC_OK);

        newRequest(get)
                .withHttpClient(httpClient)
                .onSuccess(SUCCESSFUL_HANDLER)
                .execute(10);

        verify(SUCCESSFUL_HANDLER).handle();
        verify(httpClient).executeMethod(get);

    }

    @Test
    public void shouldCallGeneralErrorHandlerWhenAnyError() throws Exception {
        when(httpClient.executeMethod(get)).thenReturn(SC_NOT_FOUND);

        newRequest(get)
                .withHttpClient(httpClient)
                .onSuccess(SUCCESSFUL_HANDLER)
                .onAnyError(GENERAL_ERROR_HANDLER)
                .execute(10);

        verify(GENERAL_ERROR_HANDLER).handle();
        verifyNoMoreInteractions(SUCCESSFUL_HANDLER);
        verify(httpClient).executeMethod(get);
    }


    @Test
    public void shouldCallSpecificErrorHandlerWHenSpecificErrorHandlerRegistered() throws Exception {
        when(httpClient.executeMethod(get)).thenReturn(SC_NOT_FOUND);

        newRequest(get)
                .withHttpClient(httpClient)
                .onSuccess(SUCCESSFUL_HANDLER)
                .onAnyError(GENERAL_ERROR_HANDLER)
                .onRespond(SC_NOT_FOUND, SPECIFIC_ERROR_HANDLER)
                .execute(10);

        verify(SPECIFIC_ERROR_HANDLER).handle();
        verifyNoMoreInteractions(SUCCESSFUL_HANDLER);
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(get);

    }

    @Test(expected = InvalidParameterException.class)
    public void shouldRaiseExceptionWhenTryCallExecuteWithoutAValidInitialRequest() throws Exception {

        when(httpClient.executeMethod(get)).thenReturn(SC_NOT_FOUND);

        newRequest(null)
                .withHttpClient(httpClient)
                .onSuccess(SUCCESSFUL_HANDLER)
                .onAnyError(GENERAL_ERROR_HANDLER)
                .onRespond(SC_NOT_FOUND, SPECIFIC_ERROR_HANDLER)
                .execute(10);

        verify(SPECIFIC_ERROR_HANDLER).handle();
        verifyNoMoreInteractions(SUCCESSFUL_HANDLER);
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(get);
    }


    @Test
    public void shouldSetHeadersForOutGoingRequest() throws Exception {

        when(httpClient.executeMethod(post)).thenReturn(SC_OK);

        newRequest(post)
                .withHttpClient(httpClient)
                .withHeader(HttpHeaders.ACCEPT, "123")
                .onSuccess(SUCCESSFUL_HANDLER)
                .onAnyError(GENERAL_ERROR_HANDLER)
                .onRespond(SC_NOT_FOUND, SPECIFIC_ERROR_HANDLER)
                .execute(10);
        verify(post).setRequestHeader(HttpHeaders.ACCEPT, "123");
        verify(SUCCESSFUL_HANDLER).handle();
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(post);
    }

    @Test
    public void shouldSetRequestEntityOnlyForPost() throws Exception {

        when(httpClient.executeMethod(post)).thenReturn(SC_OK);

        newRequest(post)
                .withHttpClient(httpClient)
                .withRequestEntity(stringEntity)
                .onSuccess(SUCCESSFUL_HANDLER)
                .onAnyError(GENERAL_ERROR_HANDLER)
                .onRespond(SC_NOT_FOUND, SPECIFIC_ERROR_HANDLER)
                .execute(10);

        verify(post).setRequestEntity(stringEntity);
        verify(SUCCESSFUL_HANDLER).handle();
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(post);
    }

    @Test
    public void shouldBeNotifyHandlerWhenExceptionRaised() throws Exception {

        IOException ioException = new IOException();
        when(httpClient.executeMethod(post)).thenThrow(ioException);

        newRequest(post)
                .withHttpClient(httpClient)
                .onException(IOException.class, exceptionHandler)
                .execute(10);

        verify(exceptionHandler).handle(ioException);
        verifyNoMoreInteractions(SUCCESSFUL_HANDLER);
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(post);
    }

    @Test
    public void shouldOnlyNotifyRegisteredHandlerForSpecificException() throws Exception {

        IOException ioException = new IOException();
        when(httpClient.executeMethod(post)).thenThrow(ioException);

        newRequest(post)
                .withHttpClient(httpClient)
                .onException(IOException.class, exceptionHandler)
                .onException(SQLException.class, unexpectedExceptionHandler)
                .execute(10);

        verify(exceptionHandler).handle(ioException);
        verifyNoMoreInteractions(SUCCESSFUL_HANDLER);
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verifyNoMoreInteractions(unexpectedExceptionHandler);
        verify(httpClient).executeMethod(post);
    }


    @Test(expected = InvalidParameterException.class)
    public void shouldRaiseExceptionWhenTrySettingRequestEntityForGet() throws Exception {

        when(httpClient.executeMethod(get)).thenReturn(SC_OK);

        newRequest(get)
                .withHttpClient(httpClient)
                .withRequestEntity(stringEntity)
                .onSuccess(SUCCESSFUL_HANDLER)
                .onAnyError(GENERAL_ERROR_HANDLER)
                .onRespond(SC_NOT_FOUND, SPECIFIC_ERROR_HANDLER)
                .execute(10);

        verify(SUCCESSFUL_HANDLER).handle();
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(get);
    }

    @Test
    public void shouldReturnResposneEntityAfterSuccess() throws Exception {

        when(httpClient.executeMethod(post)).thenReturn(SC_OK);

        newRequest(post)
                .withHttpClient(httpClient)
                .withRequestEntity(stringEntity)
                .onSuccess(SUCCESSFUL_HANDLER)
                .onRespond(SC_NOT_FOUND, SPECIFIC_ERROR_HANDLER)
                .execute(10);

        verify(post).setRequestEntity(stringEntity);
        verify(SUCCESSFUL_HANDLER).handle();
        verifyNoMoreInteractions(GENERAL_ERROR_HANDLER);
        verify(httpClient).executeMethod(post);
    }


}