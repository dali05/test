   @Test
    void testBuildFinalPayload() {
        // GIVEN
        ObjectMapper mapper = new ObjectMapper();

        // Template simulé : { "foo": "bar" }
        ObjectNode template = mapper.createObjectNode();
        template.put("foo", "bar");

        // deepCopy renvoie une copie du template
        when(templatePayload.deepCopy()).thenReturn(template);

        // WHEN
        ObjectNode result = service.buildFinalPayload("ABC123");

        // THEN
        assertNotNull(result);
        assertEquals("bar", result.get("foo").asText());
        assertEquals("ABC123", result.get("vptoken").asText());

        // Vérifier que le template original n’est pas modifié
        assertFalse(templatePayload.has("vptoke