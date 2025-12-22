setUserInfo(
  state: WritableDraft<User>,
  action: PayloadAction<UserProfile>
): void {
  const habilitations = Array.isArray(action.payload.habilitations)
    ? action.payload.habilitations
    : [];

  const roles: Role[] = Array.isArray(habilitations?.[0]?.roles)
    ? habilitations[0].roles
    : [];

  const storedRole = localStorage.getItem("role");

  state.userName = action.payload.name ?? "User";

  if (storedRole) {
    state.isAdmin = storedRole === "ADMIN";
    state.isAudit = storedRole === "AUDIT";
    state.navigationTab = navigation.filter(
      nav => nav.role === storedRole
    );
    return;
  }

  const hasAdmin = roles.some(r => r?.libelle === "ADMIN");
  const hasAudit = roles.some(r => r?.libelle === "AUDIT");

  state.isAdmin = hasAdmin;
  state.isAudit = hasAudit;

  if (hasAudit) {
    state.navigationTab = navigation.filter(nav => nav.role === "AUDIT");
  } else if (hasAdmin) {
    state.navigationTab = navigation.filter(nav => nav.role === "ADMIN");
  } else {
    state.navigationTab = [];
  }
}