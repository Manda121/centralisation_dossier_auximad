<script setup>
import LoginForm from '../components/LoginForm.vue'
</script>

<template>
  <div class="login-page">
    <!-- Image / visuel à gauche (75 %) -->
    <div class="login-page__image">
      <img src="../../public/images/still-life-documents-stack.jpg" alt="AUXIMAD" class="login-page__img" />
    </div>

    <!-- Formulaire à droite (25 %) -->
    <div class="login-page__form">
      <LoginForm />
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  width: 100%;
  background: #f4f6f9;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
}

/* ── Image (gauche, 75 %) ── */
.login-page__image {
  flex: 1; /* occupe tout l'espace restant → 75 % */
  overflow: hidden; /* pour que l'image ne dépasse pas */
  background: #d5dce6; /* couleur de fond au cas où */
}

.login-page__img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* recadre l'image pour remplir le conteneur sans déformation */
  display: block;
}

/* ── Formulaire (droite, 25 %) ── */
.login-page__form {
  flex: 0 0 25%; /* largeur fixe à 25 % */
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 20px;
  /* Ombre sur le bord gauche pour séparer l'image du formulaire */
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.05);
  z-index: 1;
}

/* ── Responsive : sur petit écran, le formulaire passe au-dessus ── */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column-reverse; /* formulaire en haut, image en bas */
    height: auto;
    min-height: 100vh;
  }

  .login-page__form {
    flex: 0 0 auto;
    width: 100%;
    padding: 30px 20px;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.05);
  }

  .login-page__image {
    flex: 0 0 300px; /* hauteur fixe pour l'image sur mobile */
    width: 100%;
  }
}
</style>