when(mockLogger.isErrorEnabled()).thenReturn(true);

secureLogger.error("Authorization: Bearer {}", "abc.def.ghi");

verify(mockLogger).error(
    argThat(msg -> msg.contains("Bearer")),
    argThat(arg -> arg.toString().contains("[MASKED]") 
               || arg.toString().contains("[TOKEN_MASKED]")
               || arg.toString().contains("***MASKED***"))
);