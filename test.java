zeebeClient
    .newPublishMessageCommand()
    .messageName("walletGetRequest4IdOK")
    .correlationKey(requestId.toString())
    .variables(Map.of(
        "walletResponse", "OK",       // 💥 IMPORTANT : permet au workflow d'éviter la branche FEEL cassée
        "requestRetrieved", true      // optionnel
    ))
    .send()
    .join();


zeebeClient
    .newPublishMessageCommand()
    .messageName("receivedResponseOK")
    .correlationKey(requestId.toString())
    .variables(Map.of(
        "walletResponse", "OK",       // requis pour validation finale
        "responseReceivedAt", System.currentTimeMillis()  // optionnel
    ))
    .send()
    .join();