import { AuthProvider } from "react-oidc-context";

function App(): Element {
  const [config, setConfig] = useState<Config | null>(null);

  useEffect(() => {
    fetchConfig().then(setConfig);
  }, []);

  if (!config) {
    return <Box>Loading...</Box>;
  }

  return (
    <AuthProvider {...config.oidcConfig}>
      <ConfigProvider config={config}>
        <Wrapper>
          <LocalizationProvider
            dateAdapter={AdapterDayjs}
            adapterLocale={locale.split("-")[0]}
          >
            <ThemeProvider theme={theme}>
              <ObiModalProvider>
                <CssBaseline />
                <ErrorNotificationController />
                <RouterProvider router={router} />
              </ObiModalProvider>
            </ThemeProvider>
          </LocalizationProvider>
        </Wrapper>
      </ConfigProvider>
    </AuthProvider>
  );
}


const oidcConfig: UserManagerSettings = {
  authority: import.meta.env.VITE_OIDC_AUTHORITY,
  client_id: import.meta.env.VITE_OIDC_CLIENT_ID,
  redirect_uri: import.meta.env.VITE_OIDC_REDIRECT_URI,
  response_type: "code",
  scope: import.meta.env.VITE_OIDC_SCOPE,
  loadUserInfo: true,
  automaticSilentRenew: false,
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};