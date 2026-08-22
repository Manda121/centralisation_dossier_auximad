# Architecture OOP - Système de Login et Utilisateur

## Vue d'ensemble

Cette architecture suit le pattern **Model-Service-Component** où:
- **Models** : Encapsulent les données et la logique métier
- **Services** : Gèrent l'interaction entre les models et l'API
- **Components** : Affichent l'interface et interagissent avec les services

## Structure

```
frontend/src/
├── models/
│   └── Utilisateur.js          # Modèle de l'utilisateur
├── services/
│   └── UtilisateurService.js   # Service métier utilisateur
├── utils/
│   └── auth.service.js         # Service API d'authentification
├── components/
│   └── LoginForm.vue           # Composant du formulaire de login
└── views/
    └── LoginView.vue           # Vue du login
```

## Modèle Utilisateur (`Utilisateur.js`)

Le modèle encapsule:
- **Propriétés** : Tous les attributs de l'utilisateur (id, nomUtilisateur, nom, prenom, email, etc.)
- **Méthodes métier** :
  - `getNomComplet()` : Retourne le nom complet formaté
  - `validerConnexion()` : Valide les données de connexion
  - `validerProfil()` : Valide le profil complet
  - `estEmailValide()` : Valide le format Email
  - `chargerDonnees()` : Charge les données depuis un objet
  - `toJSON()` : Convertit en objet JavaScript
  - `reinitialiser()` : Réinitialise les données

### Exemple d'utilisation

```javascript
import Utilisateur from '@/models/Utilisateur'

// Créer un nouvel utilisateur
const utilisateur = new Utilisateur()

// Charger des données
utilisateur.chargerDonnees({
    id: 1,
    nomUtilisateur: 'jean.dupont',
    nom: 'Dupont',
    prenom: 'Jean',
    email: 'jean.dupont@example.com',
    role: 'admin',
    agence: 'Paris'
})

// Utiliser les méthodes métier
console.log(utilisateur.getNomComplet()) // "Jean Dupont"

// Valider
const validation = utilisateur.validerConnexion('jean.dupont', 1, 'password123')
if (validation.valide) {
    console.log('Données valides')
} else {
    console.log(validation.erreurs)
}
```

## Service Métier (`UtilisateurService.js`)

Le service fait le lien entre les models et l'API:
- **Authentification** : `authentifier(nomUtilisateur, codeAgence, motDePasse)`
- **Récupération** : `obtenirUtilisateurConnecte()`
- **Déconnexion** : `deconnecter()`
- **Vérification** : `estConnecte()`
- **Formatage** : `formaterPourAffichage(utilisateur)`

### Exemple d'utilisation

```javascript
import UtilisateurService from '@/services/UtilisateurService'

// Authentifier un utilisateur
try {
    const utilisateur = await UtilisateurService.authentifier(
        'jean.dupont',
        1,
        'password123'
    )
    console.log(utilisateur.getNomComplet()) // "Jean Dupont"
} catch (error) {
    console.error(error.message)
}

// Récupérer l'utilisateur connecté
const current = UtilisateurService.obtenirUtilisateurConnecte()
if (current) {
    console.log(current.email)
}

// Vérifier la connexion
if (UtilisateurService.estConnecte()) {
    console.log('Utilisateur connecté')
}
```

## Composant Login Refactorisé

Le composant utilise maintenant le Service pour:
1. Créer une instance du modèle
2. Valider les données via les méthodes du modèle
3. Appeler le service métier pour l'authentification
4. Charger les données de réponse dans le modèle

```javascript
import Utilisateur from '../models/Utilisateur'
import UtilisateurService from '../services/UtilisateurService'

// Créer une instance
const utilisateur = new Utilisateur()

// Valider via le modèle
const validation = utilisateur.validerConnexion(
    credentials.nomUtilisateur,
    codeAgence,
    credentials.motDePasse
)

// Utiliser le service
const response = await UtilisateurService.authentifier(
    credentials.nomUtilisateur,
    credentials.codeAgence,
    credentials.motDePasse
)
```

## Avantages de cette architecture

✅ **Séparation des responsabilités** : Chaque classe a un seul rôle
✅ **Réutilisabilité** : Les models et services peuvent être utilisés dans plusieurs composants
✅ **Testabilité** : Facile de tester la logique métier indépendamment
✅ **Maintenabilité** : Toute la logique métier est centralisée
✅ **Évolutivité** : Facile d'ajouter de nouvelles fonctionnalités

## Prochaines étapes

Pour étendre cette architecture:

1. **Ajouter d'autres models** (Role, Agence, etc.)
2. **Créer des services** pour chaque entité
3. **Utiliser les models** dans tous les composants
4. **Ajouter des tests unitaires** pour les models et services

### Exemple : Modèle Role

```javascript
export class Role {
    constructor(data = {}) {
        this.id = data.id || null
        this.nom = data.nom || ''
        this.description = data.description || ''
    }

    isAdmin() {
        return this.nom === 'ADMIN'
    }
}
```

### Exemple : Modèle Agence

```javascript
export class Agence {
    constructor(data = {}) {
        this.id = data.id || null
        this.code = data.code || ''
        this.nom = data.nom || ''
        this.ville = data.ville || ''
    }

    getCodeFormatted() {
        return `AG-${this.code}`
    }
}
```
