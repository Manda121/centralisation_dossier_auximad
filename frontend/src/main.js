import { createApp } from 'vue'
import './assets/main.css'
import App from './App.vue'
import router from './router/index.js' // Importe le fichier que nous venons de créer

const app = createApp(App)

app.use(router) // Dis à Vue d'utiliser le routeur
app.mount('#app')