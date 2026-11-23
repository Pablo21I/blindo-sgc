# Blindo SGC — Sistema Móvil de Seguridad Industrial

Aplicación móvil Android (**Kotlin + Retrofit 2 + OkHttp Interceptor + Room**) integrada con una API REST protegida en **Laravel 12 + MySQL + Laravel Sanctum** para el control de seguridad industrial (validación Poka-Yoke de EPP bajo NOM-017-STPS, matriz SIMOPS, órdenes activas, bitácora auditable con evidencia fotográfica y generación de Permisos de Trabajo en PDF).

---

## Estructura del Repositorio

```text
Blindo/
├── app/                          # Aplicación Android (Kotlin, ViewBinding, Material 3, Retrofit + OkHttp)
├── backend-laravel/              # API RESTful en Laravel 12 + Laravel Sanctum + Migraciones MySQL
├── DOCUMENTACION_BLINDO_SGC.html # Documentación técnica imprimible a PDF (diagrama arquitectónico y retos)
└── README.md
```

---

## Arquitectura y Características Principales

### Backend (`backend-laravel/`)
- **Base de datos MySQL** con 4 tablas relacionadas + tokens de Sanctum:
  - `users` (cuentas de acceso con contraseña cifrada bcrypt)
  - `personals` (operadores y supervisores ligados a `users`)
  - `asignacion_epps` (inventario y vigencia de EPP ligado a `personals`)
  - `validaciones` (órdenes activas `activa = 1` y bitácora histórica `activa = 0`)
  - `personal_access_tokens` (gestión de tokens Bearer con Laravel Sanctum)
- **Endpoints RESTful (`/api/...`)**:
  - `POST /api/register`, `POST /api/login`, `POST /api/logout`, `GET /api/me`
  - `GET / POST / PUT / DELETE /api/personal`
  - `GET / POST / PUT / DELETE /api/asignaciones-epp`
  - `GET / POST / PUT / DELETE /api/validaciones` y `PUT /api/validaciones/{id}/finalizar`
- **Pruebas automatizadas**: `tests/Feature/BlindoApiTest.php` valida flujo completo Sanctum, CRUD y códigos HTTP `401` / `422`.

### Aplicación Móvil Android (`app/`)
- **Autenticación persistente**: `SessionManager` guarda el token Sanctum en `SharedPreferences` y `AuthInterceptor` inyecta automáticamente `Authorization: Bearer <token>` y `Accept: application/json` en cada petición Retrofit.
- **Operaciones CRUD en tiempo real**: Gestión de Personal, Asignación/Renovación de EPP, Validación de Seguridad (Semáforo Verde/Amarillo/Rojo), Órdenes Activas y Bitácora contra MySQL.
- **Manejo de errores robusto (`ApiErrorParser`)**: Sin crasheos ante caídas de servidor, timeouts, credenciales inválidas (`401`) o errores de validación por campo (`422`).
- **Evidencia fotográfica y Permisos PDF STPS**: Captura desde cámara/galería visible en Bitácora e incrustada en el permiso PDF generado de forma nativa con `PdfDocument`.

---

## Guía Rápida de Ejecución Local

### 1. Backend Laravel (XAMPP / MySQL)
```bash
cd backend-laravel
composer install
cp .env.example .env
php artisan key:generate
# Configurar DB_CONNECTION=mysql y DB_DATABASE=blindo en .env
php artisan migrate
php artisan serve --host=0.0.0.0 --port=8000
```

### 2. Aplicación Android (Android Studio)
1. Abrir la carpeta raíz en **Android Studio** y sincronizar Gradle.
2. Verificar la URL base en `app/src/main/java/com/example/blindo/data/remote/ApiConfig.kt` (por defecto `http://10.0.2.2:8000/api/` para emulador Android Studio).
3. Ejecutar en emulador o dispositivo físico (**Run ▶**).
