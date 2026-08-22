# 🐾 Eureka

Aplicación móvil para reportar mascotas perdidas y encontradas, desarrollada como Trabajo Integrador para la materia **Aplicaciones Móviles** — Universidad Nacional Arturo Jauretche.

---

## 📱 Descripción

Eureka resuelve un problema real: hoy cuando alguien pierde o encuentra una mascota, el proceso es caótico — grupos de WhatsApp, publicaciones en Facebook, carteles en la calle. La información está dispersa y no tiene organización geográfica.

Eureka centraliza los reportes en una sola app: el usuario publica en segundos con foto, descripción y ubicación automática. Todos los reportes se ven en un mapa en tiempo real, y el sistema envía notificaciones cuando aparece un reporte cerca.

---

## 🛠️ Stack tecnológico

### App nativa (Android)
- **Kotlin** + **Android Studio**
- **Firebase Authentication** — registro e inicio de sesión con email y contraseña
- **Firestore** — base de datos en tiempo real con listeners activos
- **Firebase Storage** — almacenamiento de imágenes (versión nativa)
- **Cloudinary** — almacenamiento de imágenes
- **OSMDroid** — mapas basados en OpenStreetMap
- **Glide** — carga de imágenes
- **Arquitectura MVVM** con `StateFlow`, `ViewModel` y `Fragment`

## ✅ Requerimientos implementados

| # | Requerimiento | Estado |
|---|---------------|--------|
| RF1 | Autenticación con Firebase | ✅ |
| RF2 | Listado de reportes del usuario | ✅ |
| RF3 | Detalle del reporte | ✅ |
| RF4 | Crear y editar reportes | ✅ |
| RF5 | Geolocalización con mapa | ✅ |
| RF6 | Cámara y galería | ✅ |
| RF7 | Notificaciones locales por proximidad | ✅ |

---

## 🚀 Cómo correr el proyecto

### App nativa (Android)

**Requisitos:**
- Android Studio Hedgehog 2023.1.1 o posterior
- JDK 11
- Emulador con API 26 o superior, o dispositivo físico

**Pasos:**

1. Cloná el repositorio
```bash
git clone https://github.com/[usuario]/eureka-android.git
cd eureka-android
```

2. Abrí el proyecto en Android Studio: `File → Open → seleccioná la carpeta`

3. Configurá Firebase:
   - Creá un proyecto en [console.firebase.google.com](https://console.firebase.google.com)
   - Registrá la app con el package `com.catedra.eureka`
   - Descargá `google-services.json` y copialo en `app/`
   - Habilitá **Email/Password** en Authentication
   - Creá la colección `reportes` en Firestore

4. Configurá Cloudinary en `local.properties`:
```
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
```

5. Ejecutá con `Run → Run 'app'` o `Shift + F10`

---

## 🔐 Reglas de Firestore

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /reportes/{reporteId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null
        && request.auth.uid == resource.data.usuarioId;
    }
    match /usuarios/{usuarioId} {
      allow read, write: if request.auth != null
        && request.auth.uid == usuarioId;
    }
  }
}
```

---

## ⚠️ Archivos excluidos del repositorio

Los siguientes archivos contienen claves y **no deben subirse al repositorio**:

- `app/google-services.json` — configuración de Firebase para Android
- `.env` — variables de entorno para la app híbrida
- `local.properties` — claves de Cloudinary para Android

Seguí los pasos de configuración arriba para obtenerlos.

---

## 📁 Estructura del proyecto (app nativa)

```
app/
├── data/
│   ├── model/          # Entidades (Reporte, Usuario)
│   └── services/       # AuthService, ReporteService, UsuarioService, AlertaService
├── ui/
│   ├── home/           # HomeFragment + HomeViewModel
│   ├── login/          # LoginFragment + LoginViewModel
│   ├── reportes/       # ReportesFragment, CrearReporte, Adapter
│   ├── detalles/       # DetallesFragment + DetallesViewModel
│   ├── mapa/           # MapFragment + MapViewModel
│   └── cuenta/         # CuentaFragment + CuentaViewModel
└── utils/              # NavegacionHelper, NotificacionHelper, IdiomaHelper, etc.
```

---

