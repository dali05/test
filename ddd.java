-- Création du schéma applicatif
CREATE SCHEMA IF NOT EXISTS admin;

-- Droits
GRANT ALL ON SCHEMA admin TO admin;

-- Optionnel : schéma par défaut
ALTER ROLE admin SET search_path TO admin, public;


psql "$DATABASE_URL" -f /sql/init.sq