SELECT datname, pg_catalog.pg_get_userbyid(datdba)
FROM pg_database
WHERE datname = 'ibmclouddb';