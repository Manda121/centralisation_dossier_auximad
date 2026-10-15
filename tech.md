Choix des technologies	
JPA
Vue.js
Vite
Bootstrap 5
Spring Boot
REST API
Choix de langage	
Java
Choix de base de données	
Postgresql

# Technologies utilisées

## Présentation générale

L’application de centralisation des dossiers Auximad est une application web organisée en trois parties : une interface utilisateur, une API métier et une base de données relationnelle. L’interface échange avec l’API par des requêtes HTTP et reçoit des données au format JSON. L’API applique les règles de gestion et accède à la base de données. Les documents transmis par les utilisateurs sont enregistrés sur le système de fichiers du serveur, tandis que leurs informations sont conservées dans la base.

## Technologies principales

| Couche | Technologies | Rôle dans l’application |
| --- | --- | --- |
| Interface web | JavaScript, Vue.js 3, Vue Router 5 | Construction des pages, des composants et de la navigation côté client. |
| Présentation | HTML, CSS, Bootstrap 5 | Mise en page, formulaires et composants visuels. |
| Développement de l’interface | Node.js, npm, Vite 7 | Gestion des dépendances, serveur de développement et compilation de l’interface. |
| Communication | Axios, HTTP, JSON | Échanges entre l’interface et l’API REST. |
| Serveur | Java 17, Spring Boot 3.3, Spring Web | Exposition des API et traitement des demandes de l’interface. |
| Accès aux données | Spring Data JPA, Hibernate | Correspondance entre les entités Java et les tables, requêtes et persistance. |
| Base de données | PostgreSQL, SQL | Stockage des utilisateurs, clients, ordres de transit, dossiers, mouvements, documents et historiques. |
| Sécurité | Spring Security, JWT (JJWT), BCrypt | Authentification, contrôle d’accès aux API, jetons de session et hachage des mots de passe. |
| Documentation des API | OpenAPI, Swagger UI (springdoc) | Description et consultation interactive des endpoints de l’API. |
| Construction et tests | Maven, JUnit 5, Spring Boot Test, MockMvc | Compilation du serveur et vérification des règles métier et des contrôleurs. |

Les numéros indiqués correspondent aux versions déclarées dans les fichiers de configuration du projet. Le fichier `frontend/package.json` prévoit Node.js `^20.19.0` ou `>=22.12.0`.

## Organisation technique

L’interface est développée avec des composants et des vues Vue (`frontend/src/components` et `frontend/src/views`). Vue Router gère les pages et leurs paramètres. Des services JavaScript regroupent les appels HTTP vers le serveur ; Axios transmet les données et le jeton d’authentification.

Le serveur suit une organisation en contrôleurs, services métier, dépôts de données et entités Java (`backend/src/main/java/com/auximad`). Les contrôleurs exposent les routes REST, les services appliquent les règles métier et les dépôts Spring Data JPA effectuent les opérations de persistance. Cette organisation couvre notamment la gestion des clients, des ordres de transit, des dossiers, des marchandises, des mouvements, des documents et des échanges entre agences.

Le schéma PostgreSQL est défini dans des scripts SQL (`backend/bdd`). La configuration JPA désactive la modification automatique du schéma par Hibernate. Une recherche textuelle sur les dossiers s’appuie également sur les fonctions et index GIN de PostgreSQL.

## Fonctions et outils complémentaires

- **Documents et exports :** Spring Web prend en charge l’envoi de fichiers ; les documents sont stockés dans `backend/uploads/documents`. Côté interface, `html2pdf.js` sert à produire des PDF et `JSZip` à générer des fichiers Excel au format XLSX.
- **Assistant conversationnel :** un module Java communique avec une instance locale d’Ollama par HTTP. Sa configuration prévoit le modèle `qwen2.5:3b`. Il combine une base de connaissances interne, des données de l’application et des outils métier. Son fonctionnement dépend de la disponibilité d’Ollama et du modèle sur la machine qui héberge le service.
- **Traçabilité :** un historique des actions et un intercepteur d’audit enregistrent les opérations effectuées via l’API.

## Sécurité et validation

L’utilisateur se connecte au serveur, qui émet un jeton JWT. L’interface transmet ce jeton dans l’en-tête `Authorization` des requêtes protégées. Spring Security vérifie l’authentification, tandis que les règles d’autorisation tiennent compte des rôles et des niveaux d’accès. Les mots de passe sont hachés avec BCrypt.

Le projet contient des tests Java fondés sur JUnit 5 et Spring Boot Test, avec MockMvc pour certains contrôleurs. Un test côté interface utilise aussi le module natif `node:test` de Node.js. Ces tests portent notamment sur les règles de gestion des dossiers, les mouvements et les confirmations de suppression.
