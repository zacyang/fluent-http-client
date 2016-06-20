package com.yang.httpClient.handler;

public interface SuccessResponseHandler<S>  {
    <X> X handle(S s) throws Exception;
}
