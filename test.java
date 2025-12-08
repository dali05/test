Field connectorField = webClient.getClass().getDeclaredField("connector");
connectorField.setAccessible(true);
System.out.println("Connector = " + connectorField.get(webClient));