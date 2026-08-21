# Guide Rapide - Système OOP Login

## Fichiers créés

```
frontend/src/
├── models/
│   └── Utilisateur.js                    # ✅ NOUVEAU - Modèle de l'utilisateur
├── services/
│   └── UtilisateurService.js             # ✅ NOUVEAU - Service métier
├── components/
│   └── LoginForm.vue                     # ✅ REFACTORISÉ
│   └── inc/
│       └── Header.vue                    # ✅ REFACTORISÉ
├── ARCHITECTURE_OOP.md                   # ✅ NOUVEAU - Documentation
└── EXEMPLES_UTILISATION.md              # ✅ NOUVEAU - Exemples pratiques
```

## Utilisation Rapide

### 1️⃣ Dans un Formulaire
```javascript
import Utilisateur from '@/models/Utilisateur'
import UtilisateurService from '@/services/UtilisateurService'

const utilisateur = new Utilisateur()

// Valider avant d'envoyer
const { valide, erreurs } = utilisateur.validerConnexion(
  nomUtilisateur,
  codeAgence,
  motDePasse
)

if (!valide) {
  console.error(erreurs)
  return
}

// Authentifier
try {
  const user = await UtilisateurService.authentifier(
    nomUtilisateur,
    codeAgence,
    motDePasse
  )
  console.log(user.getNomComplet()) // "Jean Dupont"
} catch (err) {
  console.error(err.message)
}
```

### 2️⃣ Pour Afficher l'Utilisateur Connecté
```javascript
const utilisateur = UtilisateurService.obtenirUtilisateurConnecte()

if (utilisateur) {
  console.log(utilisateur.getNomComplet())
  console.log(utilisateur.email)
  console.log(utilisateur.role)
}
```

### 3️⃣ Vérifier la Connexion
```javascript
if (UtilisateurService.estConnecte()) {
  // L'utilisateur est connecté
}
```

### 4️⃣ Déconnecter
```javascript
UtilisateurService.deconnecter()
```

## Modèle Utilisateur - Propriétés

| Propriété | Type | Description |
|-----------|------|-------------|
| `id` | number | ID unique |
| `nomUtilisateur` | string | Identifiant unique (50 chars max) |
| `nom` | string | Nom de famille (100 chars max) |
| `prenom` | string | Prénom (100 chars max) |
| `email` | string | Email (100 chars max) |
| `motDePasse` | string | Mot de passe hashé |
| `role` | object | Rôle de l'utilisateur |
| `agence` | object | Agence de l'utilisateur |
| `dateCreation` | timestamp | Date de création |
| `dateModification` | timestamp | Dernière modification |

## Modèle Utilisateur - Méthodes

| Méthode | Retour | Description |
|---------|--------|-------------|
| `getNomComplet()` | string | Retourne "Prenom Nom" |
| `validerConnexion(nom, code, pwd)` | { valide, erreurs } | Valide les données de login |
| `validerProfil()` | { valide, erreurs } | Valide le profil complet |
| `estEmailValide()` | boolean | Valide le format email |
| `chargerDonnees(data)` | void | Charge les données |
| `toJSON()` | object | Convertit en objet |
| `reinitialiser()` | void | Réinitialise les données |

## Service Métier - Méthodes

| Méthode | Retour | Description |
|---------|--------|-------------|
| `authentifier(nom, code, pwd)` | Promise<Utilisateur> | Connecte l'utilisateur |
| `obtenirUtilisateurConnecte()` | Utilisateur\|null | Récupère l'utilisateur actuel |
| `estConnecte()` | boolean | Vérifie la connexion |
| `deconnecter()` | void | Déconnecte |
| `formaterPourAffichage(user)` | object | Formate pour l'affichage |

## Comparaison Avant/Après

### ❌ AVANT (Mélange logique et présentation)
```javascript
// LoginForm.vue
const credentials = { nomUtilisateur: '', motDePasse: '' }

const handleLogin = async () => {
  // Logique de validation mélangée au composant
  if (!credentials.nomUtilisateur) {
    error.value = 'Nom requis'
    return
  }
  
  // Appel direct au service API
  const response = await authService.login(...)
  // Gestion directe du stockage
  localStorage.setItem('token', response.token)
}
```

### ✅ APRÈS (Séparation claire)
```javascript
// LoginForm.vue
const utilisateur = new Utilisateur()

const handleLogin = async () => {
  // Validation via le modèle
  const { valide, erreurs } = utilisateur.validerConnexion(...)
  if (!valide) return
  
  // Authentification via le service
  const user = await UtilisateurService.authentifier(...)
  // Données automatiquement stockées par le service
}
```

## Procédure d'Extension

Pour ajouter une nouvelle entité (Role, Agence, etc.):

1. **Créer le modèle** (`src/models/Role.js`)
```javascript
export class Role {
  constructor(data = {}) {
    this.id = data.id || null
    this.nom = data.nom || ''
  }
  
  estAdmin() {
    return this.nom === 'ADMIN'
  }
}
```

2. **Créer le service** (`src/services/RoleService.js`)
```javascript
class RoleService {
  async obtenirTous() {
    // Appel API
  }
  
  async obtenirParId(id) {
    // Appel API
  }
}
```

3. **Utiliser dans les composants**
```javascript
import RoleService from '@/services/RoleService'

const roles = await RoleService.obtenirTous()
```

## Bonnes Pratiques

✅ **Valider toujours** côté client avant d'envoyer
✅ **Utiliser les services** pour toute logique métier  
✅ **Charger les données** au montage du composant
✅ **Garder les composants légers** et focalisés sur l'UI
✅ **Tester les models** indépendamment
✅ **Documenter les méthodes** avec des JSDoc

## Dépannage

### ❓ L'utilisateur n'apparaît pas dans le Header
```javascript
// Vérifier que le service est utilisé
const utilisateur = UtilisateurService.obtenirUtilisateurConnecte()
console.log(utilisateur) // Doit retourner l'objet
```

### ❓ Les erreurs de validation ne s'affichent pas
```javascript
const { valide, erreurs } = utilisateur.validerConnexion(...)
console.log(erreurs) // Voir les messages d'erreur
```

### ❓ Le modèle n'a pas les bonnes données
```javascript
utilisateur.chargerDonnees(responseData)
// Vérifier que responseData contient les bonnes clés
```

## Ressources

📖 **Documentation complète** → `ARCHITECTURE_OOP.md`
📚 **Exemples détaillés** → `EXEMPLES_UTILISATION.md`
🔗 **Code source**:
  - [Utilisateur.js](src/models/Utilisateur.js)
  - [UtilisateurService.js](src/services/UtilisateurService.js)
  - [LoginForm.vue](src/components/LoginForm.vue)
