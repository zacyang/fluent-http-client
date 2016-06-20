# fluent-http-client
`A light wrapper on top of spring resttemplate, serve as a fascade, and convert the orginal exception manner response handling into fluent style`

## The problem
When we using any httClient in rest surrounded microservice environments, in most cases we will handle more sophisticated scenario than just simple request/response transaction.

For instance, we will need do something which based on the result of some other service.
```
try {
            ResponseEntity<Object> result = this.restTemplate.exchange(requestUrl, initMethod, httpEntity, new ParameterizedTypeReference<Object>() {
            });
        } catch (RestClientException e) {
            if (e instanceof HttpStatusCodeException) {
                if (HttpStatus.NOT_FOUND == ((HttpStatusCodeException) e).getStatusCode()) {
                    dosomething();
                }
                ....
            }
        }
```
Not mention the code is not pretty, just thinking about how can we add the dosomething() in case it is another call based on the result.

We may never need a very deep http call chain. But the problem here is, we dont have a abstract level to represent the logic of send -> handle -> report.


## How to user

Notice result here is a generic type, it can be any type.

```
   String result = GenericHttpClient.newRequest(HttpMethod.GET, url).
                withHeader(HttpHeaders.ACCEPT, "application/txt").
                withRestTemplate(restTemplate).
                onException(registeredExceptionHandlerType, exceptionHandler).
                onRespond(HttpStatus.NOT_FOUND, specificRespCodeHandler).
                onSuccess(successfulHandler).
                collectResponse();
```

Or, you can plug this module into your project, in most cases there will be a header/body related factory, say ```HttpEntityFactory```

which should implement ```HttpEntityFactory```

then register it though

```
GenericHttpClient.newRequest(HttpMethod.GET, url).
withEntityFactory(httpEntityFactory)
```
