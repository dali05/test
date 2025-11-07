when(mockLogger.isErrorEnabled()).thenReturn(true);

secureLogger.error("Authorization: Bearer {}", "abc.def.ghi");

verify(mockLogger).error(
    argThat(msg -> msg.contains("Bearer")),   // or contains("[TOKEN_MASKED]")
    ArgumentMatchers.<Object[]>any()          // match the varargs array
);