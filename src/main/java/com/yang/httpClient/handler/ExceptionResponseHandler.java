package com.yang.httpClient.handler;

import org.springframework.http.HttpHeaders;

public interface ExceptionResponseHandler<T>  {
    T handle(HttpHeaders responseHeaders, byte[]  responseBody) throws Exception;
}
