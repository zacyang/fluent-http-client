package com.yang.httpClient.handler;

@FunctionalInterface
public interface SuccessResponseHandler<S>  {
    <X> X handle(S s) ;
}
