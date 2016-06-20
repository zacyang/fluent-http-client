package com.yang.httpClient.handler;

import org.springframework.http.HttpHeaders;

@FunctionalInterface
public interface ExceptionResponseHandler<T>  {
    T handle(HttpHeaders responseHeaders, byte[]  responseBody) ;
}
