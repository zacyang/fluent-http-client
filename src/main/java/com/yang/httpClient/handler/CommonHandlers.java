package com.yang.httpClient.handler;

public final class CommonHandlers {
    public static final SuccessResponseHandler DEFAULT_200_OK_HANDLER = new SuccessResponseHandler() {

        public Object handle(Object o) throws Exception {
            return null;
        }
    };
}
