000-bootstrap-liquibase-schema.xml

  <?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
  xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="
    http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.32.xsd">

  <changeSet id="000-create-liquibase-schema" author="liquibase">
    <sql>
      CREATE SCHEMA IF NOT EXISTS liquibase;
    </sql>
  </changeSet>

</databaseChangeLog>


<include file="db/changelog/000-bootstrap-liquibase-schema.xml"
         relativeToChangelogFile="true"/>
