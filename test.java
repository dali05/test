WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

when(webClient.post()).thenReturn(uriSpec);
when(uriSpec.uri(anyString())).thenReturn(bodySpec);
when(bodySpec.contentType(any())).thenReturn(bodySpec);
when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
when(headersSpec.retrieve()).thenReturn(responseSpec);
when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(...));