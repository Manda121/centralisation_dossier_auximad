// src/api.js
import axios from 'axios';

// Configurations GLPI (À centraliser idéalement dans un fichier .env plus tard)
const GLPI_API_BASE = '/api-glpi/public/api.php';
const CLIENT_ID = '767ccf350d48dcc6b5f6a4a38889858bdf5fbb58ac8fb8b7922fb0dde1c167ff';
const CLIENT_SECRET = 'ef16f0fb187bcd571d54b480cba0af9a5fa4c09cd552069e436827b9096d4031';
const GLPI_USERNAME = 'glpi';
const GLPI_PASSWORD = 'glpi';

let accessToken = null;

// 1. Création de l'instance Axios
const api = axios.create({
  baseURL: GLPI_API_BASE,
  headers: {
    'Accept': 'application/json',
  }
});

// 2. Fonction isolée pour récupérer le Token OAuth2
async function fetchToken() {
  const params = new URLSearchParams();
  params.append('grant_type', 'password');
  params.append('username', GLPI_USERNAME);
  params.append('password', GLPI_PASSWORD);
  params.append('scope', 'api');

  const credentials = btoa(`${CLIENT_ID}:${CLIENT_SECRET}`);

  try {
    // On utilise axios.post directement (pas l'instance "api" pour éviter les boucles infinies)
    const response = await axios.post(`${GLPI_API_BASE}/token`, params, {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Basic ${credentials}`
      }
    });
    
    accessToken = response.data.access_token;
    return accessToken;
  } catch (error) {
    console.error("Erreur lors de la récupération du token GLPI:", error);
    throw new Error("Impossible de s'authentifier auprès de GLPI");
  }
}

// 3. Intercepteur Axios : injecte le token automatiquement avant chaque requête
api.interceptors.request.use(async (config) => {
  // Si on n'a pas encore de token, on va le chercher
  if (!accessToken) {
    await fetchToken();
  }
  
  // On ajoute le token dans les règles de l'art ITIL/OAuth2
  config.headers['Authorization'] = `Bearer ${accessToken}`;
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default api;