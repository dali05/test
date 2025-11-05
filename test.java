public class NotificationServiceImpl implements NotificationService {

    private final AccessRequestRepositoryPort requestRepository;
    private final AdminClientPort adminClient;
    private final ApigeeNotifierPort apigeeNotifier;

    public NotificationServiceImpl(AccessRequestRepositoryPort requestRepository,
                                   AdminClientPort adminClient,
                                   ApigeeNotifierPort apigeeNotifier) {
        this.requestRepository = requestRepository;
        this.adminClient = adminClient;
        this.apigeeNotifier = apigeeNotifier;
    }

    @Override
    public void sendNotification(NotificationRequest notif) {
        var config = requestRepository.findCaseIdAndConfigIdById(notif.getRequestId())
                .orElseThrow(() -> new IllegalStateException("Config not found"));
        var apigeeUrl = adminClient.getCallbackUrl(config.caseId());
        apigeeNotifier.sendToApigee(notif, apigeeUrl);
    }
}

@Component
@RequiredArgsConstructor
public class AdminClientAdapter implements AdminClientPort {

    private final RestTemplate restTemplate;

    @Override
    public String getCallbackUrl(UUID caseId) {
        // Appel HTTP vers admin service
    }
}


@Service
@RequiredArgsConstructor
public class ApigeeNotifierAdapter implements ApigeeNotifierPort {

    private final RestTemplate restTemplate;

    @Override
    public void sendToApigee(NotificationRequest notif, String apigeeUrl) {
        HttpEntity<NotificationRequest> entity = new HttpEntity<>(notif);
        restTemplate.postForEntity(apigeeUrl, entity, String.class);
    }
}

@Configuration
public class AppConfig {

    @Bean
    public NotificationService notificationService(AccessRequestRepositoryPort repo,
                                                   AdminClientPort adminClient,
                                                   ApigeeNotifierPort notifier) {
        return new NotificationServiceImpl(repo, adminClient, notifier);
    }
}

