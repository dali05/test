const oidcConfig: UserManagerSettings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  response_type: "code",
  scope: import.meta.env.VITE_OIDC_SCOPE,
  loadUserInfo: true,
  automaticSilentRenew: false,
  userStore: new WebStorageStateStore({ store: window.localStorage }),

  onSigninCallback: () => {
    window.history.replaceState(
      {},
      document.title,
      window.location.pathname
    );
  },
};


