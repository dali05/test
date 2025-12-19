import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { userManager } from "./config";

export default function Callback() {
  const navigate = useNavigate();

  useEffect(() => {
    userManager
      .signinRedirectCallback()
      .then(user => {
        console.log("✅ User logged in", user);
        navigate("/");
      })
      .catch(err => {
        console.error("❌ Callback error", err);
        navigate("/login");
      });
  }, [navigate]);

  return <div>Connexion en cours...</div>;
}
