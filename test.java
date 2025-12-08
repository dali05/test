@PostConstruct
public void checkProxy() {
    System.out.println("HTTP PROXY = " + System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort"));
    System.out.println("HTTPS PROXY = " + System.getProperty("https.proxyHost") + ":" + System.getProperty("https.proxyPort"));
    System.out.println("HTTP USER  = " + System.getProperty("http.proxyUser"));
    System.out.println("SYSTEM PROXY ENABLED = " + System.getProperty("java.net.useSystemProxies"));
}