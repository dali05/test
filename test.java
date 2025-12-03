    // 1) Enregistrement DB
    repo.save(...);

    // 2) Envoi du message Zeebe pour débloquer wait4RequestRetrieval
    zeebeClient
        .newPublishMessageCommand()
        .messageName("walletGetRequest4IdOK")      // nom défini dans le BPMN
        .correlationKey(requestId.toString())      // clé de corrélation
        .variables(Map.of(
            "requestId", requestId,
            "walletResponse", "OK"
        ))
        .send()
        .join();

    log.info("Message walletGetRequest4IdOK envoyé pour requestId {}", requestId);
}



