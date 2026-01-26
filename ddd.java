SELECT
  r1.rolname AS vault_user,
  r2.rolname AS member_of
FROM pg_auth_members m
JOIN pg_roles r1 ON m.member = r1.oid
JOIN pg_roles r2 ON m.roleid = r2.oid
WHERE r1.rolname = 'COLLE_ICI_LE_USERNAME_VAULT';