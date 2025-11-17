String key = Files.readString(Paths.get("src/main/resources/cert/key_pkcs8.pem"))
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");

byte[] keyBytes = Base64.getDecoder().decode(key);
PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);