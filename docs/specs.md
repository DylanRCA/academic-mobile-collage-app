# Especificación de Requerimientos y Diseño de Software

> [!NOTE]
> **Updated Canonical Documentation:**
> - [ARCHITECTURE_AND_FLOWS.md](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/docs/ARCHITECTURE_AND_FLOWS.md) - Deep Links, QR Scan, Account Recovery, Camera, and 12-photo 3D Widescreen Collage.
> - [PROJECT_STRUCTURE.md](file:///C:/Users/Dylan/AndroidStudioProjects/AppCollageFotosSNT/docs/PROJECT_STRUCTURE.md) - Complete Directory Tree, Java Source Code Breakdown, and Firebase Realtime Database Schema.

## Proyecto: Aplicación Móvil de Collage Interactivo para Eventos
**Curso:** Desarrollo de Aplicaciones Móviles  
**Plataforma de Desarrollo:** Android Studio  
**Backend:** Firebase (Authentication, Cloud Firestore)

---

## 1. Resumen Ejecutivo
El sistema es una solución móvil diseñada para capturar la interacción de los asistentes a un evento (bodas, graduaciones, fiestas corporativas). Los usuarios escanean un código QR para acceder a la aplicación, toman una fotografía con la cámara de su dispositivo, redactan un mensaje opcional y la envían para su publicación inmediata en una pantalla compartida de collage interactivo que opera en tiempo real.

---

## 2. Arquitectura General del Sistema


```

┌──────────────────────┐         ┌────────────────────────┐
│   Cliente Móvil      │         │   Cloud Firestore      │
│  (Captura & Envío)   │         │                        │
│                      ├────────>│  /events/{eventId}     │
│  - Escáner QR        │  Auth   │  /posts/{postId}       │
│  - Cámara (CameraX)  │  Write  │  (Base64 <= 800 KB)    │
│  - Compresión Bitmap │         └───────────┬────────────┘
└──────────────────────┘                     │
│ Snapshot Listener
▼ (Tiempo Real)
┌────────────────────────┐
│   Cliente Móvil        │
│   (Modo Collage / TV)  │
│                        │
│  - Hero Spotlight      │
│  - Dynamic Masonry     │
│  - Desplazamiento FIFO │
└────────────────────────┘

```

### Componentes Clave:
* **Firebase Authentication:** Gestión de sesiones anónimas vinculadas al identificador del evento extraído del código QR.
* **Cloud Firestore:** Base de datos reactiva para almacenar la metadata de los eventos y las publicaciones (incluyendo las imágenes codificadas en Base64).
* **Módulo de Procesamiento de Imagen:** Pipeline local en Android para escalar y comprimir el mapa de bits antes de la codificación Base64, evitando exceder la cuota técnica de 1 MB por documento de Firestore.

---

## 3. Flujo de Eventos del Usuario

1. **Escaneo del Código QR:**
    * El usuario abre la app y apunta la cámara al código QR provisto en el recinto del evento.
    * La app decodifica el payload (JSON o URI con `eventId` y token/clave).
2. **Autenticación e Inicialización:**
    * La app autentica al usuario mediante Firebase Auth (sesión anónima/credenciales temporales).
    * Se comprueba la vigencia y estado (`active: true`) del evento en Firestore.
3. **Solicitud de Permisos y Captura:**
    * Se evalúa y solicita en tiempo de ejecución el permiso `android.permission.CAMERA`.
    * Tras la concesión, se inicializa el preview mediante CameraX y el usuario acciona el disparador.
4. **Revisión y Composición del Pie de Foto:**
    * Se visualiza la imagen capturada.
    * A la izquierda inferior se despliega el logotipo oficial del evento (obtenido de `events/{eventId}`).
    * A la derecha inferior se habilita un campo de entrada para un mensaje dedicatorio opcional (máximo 200 caracteres).
    * Opciones: **Reintentar** (descarta y vuelve a disparador) o **Enviar**.
5. **Compresión y Envío:**
    * La app redimensiona la imagen a un máximo de 1024x768 px y aplica compresión JPEG (calidad 60%–70%).
    * Se codifica en Base64 (peso final estimado: 400 KB – 600 KB).
    * Se escribe el registro en `/posts`.
    * Se redirige a la vista del collage en vivo.

---

## 4. Diseño de la Pantalla de Visualización (Collage)

La pantalla implementa el patrón **Mosaico Dinámico con Destacado Temporal (Hero + Grid)**:

### Máquina de Estados Visual:
* **Estado 1 (Espera / Grid Base):** Se renderiza una cuadrícula escalonada (*Staggered Grid*) con las últimas $N$ fotos (8 a 12 fotos visibles). Cada celda incluye el footer con el logo del evento y el mensaje correspondiente.
* **Estado 2 (Hero Spotlight - Entrada):** Al recibir un nuevo documento desde Firestore, la imagen se presenta en un contenedor central que ocupa entre el 65% y 70% del área útil, aplicando una transición suave (*Scale 0.8 -> 1.0* y *Fade-in*) con un fondo semitransparente o desenfocado.
* **Estado 3 (Hero Spotlight - Reposo):** La imagen permanece destacada entre 4 y 5 segundos para permitir la lectura del mensaje.
* **Estado 4 (Transición al Mosaico):** La imagen se reduce de escala y se anima hacia la primera celda disponible del grid base.
* **Estado 5 (Reordenamiento FIFO):** Las fotos existentes se desplazan. Si el grid ha superado su capacidad máxima $N$, el elemento más antiguo ejecuta una animación de salida (*fade-out*) y se descarta de la vista.

---

## 5. Especificación del Modelo de Datos (Cloud Firestore)

### Colección: `events`
* **Ruta:** `/events/{eventId}`

| Campo | Tipo | Requerido | Descripción |
| :--- | :--- | :---: | :--- |
| `name` | `string` | Sí | Nombre representativo del evento (ej. "Boda Mateo & Lucía"). |
| `eventType` | `string` | Sí | Clasificación del evento (ej. "boda", "graduacion"). |
| `logoBase64` | `string` | Sí | Logo en PNG/WebP codificado en Base64 (~200x200 px, < 50 KB). |
| `eventCode` | `string` | Sí | Token o identificador de acceso codificado en el QR. |
| `active` | `boolean` | Sí | Bandera de admisión de nuevas capturas. |
| `createdAt` | `timestamp` | Sí | Marca de tiempo de registro del evento en servidor. |

### Colección: `posts`
* **Ruta:** `/posts/{postId}`

| Campo | Tipo | Requerido | Restricción / Límite | Descripción |
| :--- | :--- | :---: | :--- | :--- |
| `eventId` | `string` | Sí | Coincidir con un `eventId` válido. | Identificador del evento asociado. |
| `userId` | `string` | Sí | Auth UID. | Identificador del usuario emisor. |
| `imageBase64` | `string` | Sí | Tamaño de cadena < 800 KB. | Fotografía comprimida en Base64. |
| `message` | `string` | No | Máximo 80 caracteres. | Dedicatoria o mensaje adjunto. |
| `createdAt` | `timestamp` | Sí | `serverTimestamp()` | Fecha y hora para indexación cronológica. |
| `deviceInfo` | `string` | No | Texto libre. | Metadata de hardware/sistema del cliente. |

### Índices Compuestos Requeridos
* **Colección:** `posts`
* **Campos:** `eventId` (ASC), `createdAt` (DESC)

---

## 6. Reglas de Seguridad (Firestore Rules)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    match /events/{eventId} {
      allow read: if request.auth != null;
      allow write: if false; 
    }

    match /posts/{postId} {
      allow read: if request.auth != null;
      
      allow create: if request.auth != null
        && request.resource.data.userId == request.auth.uid
        && request.resource.data.message.size() <= 80
        && request.resource.data.imageBase64.size() < 800000
        && request.resource.data.keys().hasAll(['eventId', 'userId', 'imageBase64', 'createdAt']);
        
      allow update, delete: if false;
    }
  }
}

```

---

## 7. Requerimientos Técnicos y Dependencias Recomendadas (Android)

* **Librerías Core:**
* `androidx.camera:camera-camera2`: Gestión del ciclo de vida del sensor fotográfico.
* `com.google.mlkit:barcode-scanning`: Escaneo y decodificación de códigos QR locales.
* `com.google.firebase:firebase-firestore-ktx`: Capa de datos reactiva en tiempo real.
* `com.google.firebase:firebase-auth-ktx`: Autenticación segura y reglas de acceso.
* `org.jetbrains.kotlinx:kotlinx-coroutines-android`: Despacho de compresión y decodificación de Base64 fuera del hilo principal (`Dispatchers.Default` / `Dispatchers.IO`).

