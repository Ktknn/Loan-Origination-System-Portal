import express from "express";
import path from "path";
import dotenv from "dotenv";
import { createServer as createViteServer } from "vite";
import jsforce from "jsforce";

dotenv.config();

const app = express();
const PORT = 3000;

app.use(express.json());

let activeConn: any = null;
let lastError: string | null = null;
let connectedOrgInfo: any = null;

// Helper to get active connection or attempt to auto-login using env variables
async function getOrInitConnection(): Promise<any> {
  if (activeConn) {
    try {
      // Validate session is alive by making a simple identity call
      const identity = await activeConn.identity();
      return activeConn;
    } catch (e) {
      console.log("Existing connection session stale, re-authenticating...");
      activeConn = null;
      connectedOrgInfo = null;
    }
  }

  const username = process.env.SALESFORCE_USERNAME;
  const password = process.env.SALESFORCE_PASSWORD;
  const token = process.env.SALESFORCE_SECURITY_TOKEN;
  const loginUrl = process.env.SALESFORCE_LOGIN_URL || "https://login.salesforce.com";
  const clientId = process.env.SALESFORCE_CLIENT_ID;
  const clientSecret = process.env.SALESFORCE_CLIENT_SECRET;

  console.log(`Attempting Salesforce login with url: ${loginUrl}`);

  // Flow 1: Client Credentials Flow (No Username/Password required)
  if ((!username || !password) && clientId && clientSecret) {
    console.log("Using Salesforce OAuth 2.0 Client Credentials Flow...");

    const params = new URLSearchParams();
    params.append('grant_type', 'client_credentials');
    params.append('client_id', clientId);
    params.append('client_secret', clientSecret);

    const authRes = await fetch(`${loginUrl}/services/oauth2/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });

    const data = await authRes.json();
    if (!authRes.ok) {
      throw new Error(`Client Credentials Flow failed: ${data.error_description || data.error || 'Unknown error'}`);
    }

    const conn = new jsforce.Connection({
      instanceUrl: data.instance_url,
      accessToken: data.access_token
    });

    const identity = await conn.identity();
    console.log(`✅ Salesforce Connected Successfully via Client Credentials Flow!`);
    console.log(`- Username: ${identity.username}`);
    console.log(`- Org ID: ${identity.organization_id}`);

    connectedOrgInfo = {
      username: identity.username,
      orgId: identity.organization_id,
      idUrl: identity.id,
      userId: identity.user_id,
      displayName: identity.display_name,
      instanceUrl: conn.instanceUrl,
    };

    activeConn = conn;
    lastError = null;
    return conn;
  }

  // Flow 2: Resource Owner Password Flow (Username + Password required)
  if (!username || !password) {
    throw new Error("Salesforce credentials (SALESFORCE_USERNAME, SALESFORCE_PASSWORD) not configured in environment variables.");
  }

  const connectionOpts: any = { loginUrl };
  if (clientId && clientSecret) {
    console.log("Using Salesforce Connected App Client ID & Client Secret (Resource Owner Flow)");
    connectionOpts.oauth2 = {
      loginUrl,
      clientId,
      clientSecret
    };
  }

  const conn = new jsforce.Connection(connectionOpts);

  // Login with Password + Security Token
  const fullPassword = token ? `${password}${token}` : password;
  await conn.login(username, fullPassword);

  const identity = await conn.identity();
  connectedOrgInfo = {
    username: identity.username,
    orgId: identity.organization_id,
    idUrl: identity.id,
    userId: identity.user_id,
    displayName: identity.display_name,
    instanceUrl: conn.instanceUrl,
  };

  activeConn = conn;
  lastError = null;
  return conn;
}

// Helper to construct jsforce OAuth2 client dynamically
function getOAuth2(req: express.Request): any {
  const clientId = process.env.SALESFORCE_CLIENT_ID;
  const clientSecret = process.env.SALESFORCE_CLIENT_SECRET;
  const loginUrl = process.env.SALESFORCE_LOGIN_URL || "https://login.salesforce.com";

  if (!clientId || !clientSecret) {
    throw new Error("Missing SALESFORCE_CLIENT_ID or SALESFORCE_CLIENT_SECRET in environmental variables.");
  }

  let host = req.get("host") || "localhost:3000";
  let protocol = req.headers["x-forwarded-proto"] || req.protocol || "http";
  if (host.includes(".run.app") || host.includes("ais-dev") || host.includes("ais-pre")) {
    protocol = "https";
  }

  const redirectUri = process.env.APP_URL
    ? `${process.env.APP_URL.replace(/\/$/, "")}/api/salesforce/oauth/callback`
    : `${protocol}://${host}/api/salesforce/oauth/callback`;

  console.log(`OAuth Redirect URI resolved to: ${redirectUri}`);

  return new jsforce.OAuth2({
    loginUrl,
    clientId,
    clientSecret,
    redirectUri
  });
}

// OAuth Initiative Endpoint
app.get("/api/salesforce/oauth/login", (req, res) => {
  try {
    const oauth2 = getOAuth2(req);
    const authUrl = oauth2.getAuthorizationUrl({ scope: "api id web refresh_token" });
    console.log(`Redirecting user to Salesforce OAuth page: ${authUrl}`);
    res.redirect(authUrl);
  } catch (error: any) {
    console.error("OAuth configuration error:", error);
    res.status(400).json({ success: false, message: error.message });
  }
});

// OAuth Callback Endpoint
app.get("/api/salesforce/oauth/callback", async (req, res) => {
  const { code } = req.query;
  if (!code) {
    return res.status(400).send("Missing authorization code from Salesforce.");
  }

  try {
    const oauth2 = getOAuth2(req);
    const conn = new jsforce.Connection({ oauth2 });

    console.log("Exchanging authorization code for token...");
    await conn.authorize(String(code));

    const identity = await conn.identity();
    connectedOrgInfo = {
      username: identity.username,
      orgId: identity.organization_id,
      idUrl: identity.id,
      userId: identity.user_id,
      displayName: identity.display_name,
      instanceUrl: conn.instanceUrl,
      accessToken: conn.accessToken,
      refreshToken: conn.refreshToken
    };

    activeConn = conn;
    lastError = null;

    console.log(`OAuth Login successful for user: ${identity.username}`);
    res.redirect("/");
  } catch (error: any) {
    console.error("OAuth Exchange failed:", error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// 1. Get Salesforce status
app.get("/api/salesforce/status", async (req, res) => {
  try {
    const conn = await getOrInitConnection();
    res.json({
      connected: true,
      orgInfo: connectedOrgInfo,
      lastError: null,
      authMethod: activeConn?.oauth2 ? "OAuth2 Connected App" : "Username/Password Credentials",
    });
  } catch (error: any) {
    lastError = error.message;
    res.json({
      connected: false,
      orgInfo: null,
      lastError: error.message || "Failed to authenticate with Salesforce.",
    });
  }
});

// 2. Perform direct connection
app.post("/api/salesforce/connect", async (req, res) => {
  const { username, password, token, loginUrl, clientId, clientSecret } = req.body;

  try {
    const connectionOpts: any = {
      loginUrl: loginUrl || "https://login.salesforce.com"
    };

    if (clientId && clientSecret) {
      connectionOpts.oauth2 = {
        loginUrl: loginUrl || "https://login.salesforce.com",
        clientId,
        clientSecret
      };
    }

    const conn = new jsforce.Connection(connectionOpts);

    const fullPassword = token ? `${password}${token}` : password;
    await conn.login(username, fullPassword);

    const identity = await conn.identity();
    connectedOrgInfo = {
      username: identity.username,
      orgId: identity.organization_id,
      idUrl: identity.id,
      userId: identity.user_id,
      displayName: identity.display_name,
      instanceUrl: conn.instanceUrl,
    };

    activeConn = conn;
    lastError = null;

    res.json({
      success: true,
      message: "Successfully connected to Salesforce!",
      orgInfo: connectedOrgInfo,
    });
  } catch (error: any) {
    res.status(400).json({
      success: false,
      message: error.message || "Failed to sign in to Salesforce."
    });
  }
});

// 3. Clear Connection
app.post("/api/salesforce/disconnect", (req, res) => {
  activeConn = null;
  connectedOrgInfo = null;
  lastError = null;
  res.json({ success: true, message: "Disconnected from Salesforce." });
});

// 4. Query records from Salesforce
app.get("/api/salesforce/query", async (req, res) => {
  const objectApiName = req.query.object || "Account";
  const limit = req.query.limit || 50;

  try {
    const conn = await getOrInitConnection();

    let sobjectName = String(objectApiName);

    let queryStr = `SELECT Id, Name FROM ${sobjectName} ORDER BY CreatedDate DESC LIMIT ${limit}`;
    if (sobjectName === "Account") {
      queryStr = `SELECT Id, Name, AccountNumber, Phone, AnnualRevenue FROM Account ORDER BY CreatedDate DESC LIMIT ${limit}`;
    } else if (sobjectName === "Contact") {
      queryStr = `SELECT Id, Name, Email, Phone FROM Contact ORDER BY CreatedDate DESC LIMIT ${limit}`;
    }

    const result = await conn.query(queryStr);
    res.json({
      success: true,
      records: result.records,
    });
  } catch (error: any) {
    res.status(400).json({
      success: false,
      message: error.message || `Failed to fetch records for ${objectApiName}.`
    });
  }
});

// 5. Create a record in Salesforce
app.post("/api/salesforce/upsert-record", async (req, res) => {
  const { objectName, fields } = req.body;
  if (!objectName || !fields) {
    return res.status(400).json({ success: false, message: "Missing objectName or fields in body." });
  }

  try {
    const conn = await getOrInitConnection();

    console.log(`Pushing record to SObject: ${objectName}`, fields);

    const cleanedFields: Record<string, any> = {};
    const unallowedFields = ["id", "CreatedBy", "LastModifiedBy", "attributes", "CreatedDate", "SystemModstamp"];

    for (const key of Object.keys(fields)) {
      if (!unallowedFields.includes(key) && fields[key] !== undefined && fields[key] !== null) {
        cleanedFields[key] = fields[key];
      }
    }

    const outcome = await conn.sobject(objectName).create(cleanedFields);

    res.json({
      success: true,
      outcome,
      message: `Successfully created ${objectName} in Salesforce with ID ${outcome.id}`
    });
  } catch (error: any) {
    console.error("Salesforce creation error:", error);
    res.status(400).json({
      success: false,
      message: error.message || `Failed to create record in Salesforce.`
    });
  }
});

// 6. Submit Multi-Object Application
app.post("/api/salesforce/submit-application", async (req, res) => {
  const formData = req.body;
  try {
    const conn = await getOrInitConnection();

    // 1. Create User (Applicant)
    const applicantOutcome = await conn.sobject("User__c").create({
      Name: formData.fullName,
      Email__c: formData.email,
      Birthdate__c: formData.dob,
      Address__c: formData.address,
      City__c: formData.city,
      District__c: formData.district,
      Ward__c: formData.ward,
      Status__c: "Active"
    });

    const applicantId = applicantOutcome.id;

    // 2. Create Reference Contacts
    const references = [];
    if (formData.ec1Name) {
      references.push({
        Reference_Contact_Name__c: formData.ec1Name,
        Relationship__c: formData.ec1Relation,
        Reference_Contact_Phone_Number__c: formData.ec1Phone,
        LOS_User__c: applicantId
      });
    }
    if (formData.ec2Name) {
      references.push({
        Reference_Contact_Name__c: formData.ec2Name,
        Relationship__c: formData.ec2Relation,
        Reference_Contact_Phone_Number__c: formData.ec2Phone,
        LOS_User__c: applicantId
      });
    }

    if (references.length > 0) {
      await conn.sobject("Reference_Contact__c").create(references);
    }

    // 3. Create Loan Application
    const amountNum = Number(formData.amount.replace(/[^0-9.-]+/g, ""));
    const loanOutcome = await conn.sobject("Loan_Application__c").create({
      Applicant__c: applicantId,
      Requested_Amount__c: amountNum,
      Requested_Term__c: formData.term,
      Loan_Purpose__c: formData.purpose,
      Occupation__c: formData.occupation,
      Income_Range__c: formData.income,
      Application_Status__c: "New",
      Submitted_Date__c: new Date().toISOString()
    });

    res.json({
      success: true,
      message: "Application successfully submitted to Salesforce",
      loanId: loanOutcome.id
    });

  } catch (error: any) {
    console.error("Salesforce application submission error:", error);
    res.status(500).json({
      success: false,
      message: error.message || "Failed to submit application to Salesforce."
    });
  }
});

// 7. Proxy Apex REST Calls
app.post("/api/salesforce/apex", async (req, res) => {
  const { endpoint, body, method } = req.body;
  if (!endpoint) return res.status(400).json({ success: false, message: "Missing endpoint" });

  try {
    const conn = await getOrInitConnection();
    let result;
    if (method === 'GET') {
      result = await conn.apex.get(endpoint);
    } else {
      result = await conn.apex.post(endpoint, body || {});
    }
    res.json({ success: true, data: result });
  } catch (error: any) {
    console.error("Apex call failed:", error);
    res.status(500).json({ success: false, message: error.message || "Failed to execute Apex" });
  }
});

// ─── Spring Boot LOS Backend Proxy ──────────────────────────
// Forward /api/v1/* requests to Spring Boot backend (port 8080)
const LOS_BACKEND = process.env.LOS_BACKEND_URL || "http://localhost:8080";

app.all("/api/v1/*", async (req, res) => {
  try {
    const targetUrl = `${LOS_BACKEND}${req.originalUrl}`;
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };
    // Forward Authorization header if present
    if (req.headers.authorization) {
      headers["Authorization"] = req.headers.authorization;
    }

    const fetchOptions: RequestInit = {
      method: req.method,
      headers,
    };

    if (req.method !== "GET" && req.method !== "HEAD" && req.body) {
      fetchOptions.body = JSON.stringify(req.body);
    }

    const backendRes = await fetch(targetUrl, fetchOptions);
    const text = await backendRes.text();
    let data;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = { success: false, message: text || `HTTP ${backendRes.status}` };
    }
    if (!data) {
      data = {
        success: backendRes.ok,
        message: backendRes.status === 401 ? "Chưa đăng nhập hoặc token không hợp lệ (Unauthorized)" :
                 backendRes.status === 403 ? "Truy cập bị từ chối: Cần đăng nhập với Bearer token (Forbidden)" :
                 backendRes.status === 405 ? "Phương thức không được hỗ trợ (Method Not Allowed)" :
                 `HTTP ${backendRes.status}`
      };
    }
    res.status(backendRes.status).json(data);
  } catch (error: any) {
    console.error("[LOS Proxy] Error:", error.message);
    res.status(502).json({
      success: false,
      message: `Cannot connect to LOS backend: ${error.message}`,
    });
  }
});

// Vite Middleware for handling frontend serving
async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server is running at http://localhost:${PORT}`);
  });
}

startServer();
