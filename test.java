npm init -y
npm install axios jsonwebtoken querystring dotenv
npm install --save-dev typescript ts-node @types/node @types/jsonwebtoken
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "CommonJS",
    "moduleResolution": "node",
    "strict": true,
    "esModuleInterop": true,
    "outDir": "dist"
  },
  "include": ["src"]
}
import axios from "axios";
import querystring from "querystring";
import fs from "fs";
import jwt, { Algorithm } from "jsonwebtoken";
import { randomBytes } from "crypto";

export class Jwt {

  async getAccessToken(clientId: string, authTokenUrl: string, scope: string, useCase: string) {
    const authBody = {
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: this.signAssertion(clientId, authTokenUrl, useCase),
      scope
    };

    const tokenResponse = await this.getToken(authBody, authTokenUrl);
    return tokenResponse.access_token;
  }

  private async getToken(
    body: Record<string, string>,
    url: string
  ): Promise<any> {
    const response = await axios.post(url, querystring.stringify(body), {
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
    });

    return response.data;
  }

  private signAssertion(clientId: string, authTokenUrl: string, useCase: string): string {
    const useCasePathName = useCase.toLowerCase();
    const keyPath = `cert/${useCasePathName}_truststore/key.pem`;

    const privateKey = fs.readFileSync(keyPath, "utf-8");

    const payload = {
      iat: Math.floor(Date.now() / 1000) - 30,
      exp: Math.floor(Date.now() / 1000) + 3600,
      iss: clientId,
      aud: authTokenUrl,
      sub: clientId,
      jti: randomBytes(16).toString("base64url"),
    };

    return jwt.sign(payload, privateKey, { algorithm: "RS256" });
  }
}
import { Jwt } from "./Jwt";

(async () => {
  const jwtService = new Jwt();

  const token = await jwtService.getAccessToken(
    "myClientId",
    "https://oauth-server/token",
    "my-scope",
    "abc"
  );

  console.log("ACCESS TOKEN =", token);
})();
