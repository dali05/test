zeebeClient
    .newPublishMessageCommand()
    .messageName("receivedResponseOK")   // message attendu par wait4Response
    .correlationKey(requestId)           // même requestId utilisé par le workflow
    .variables(Map.of(
        "walletResponse", "OK",          // ⚠️ cette variable est utilisée dans ton gateway !
        "responseReceivedAt", System.currentTimeMillis()
    ))
    .send()
    .join();