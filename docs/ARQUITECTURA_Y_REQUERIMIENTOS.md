# Documento de Requerimientos y Arquitectura de Software — VitalNotifier (Android)

**Curso:** Ingeniería de Software  
**Docente:** Dr. Alexander Morales Gonzales  
**Proyecto:** Ecosistema VitalStock Suite (Agente Móvil Android + Servidor Desktop)  
**Repositorios del Sistema:**
- **Agente Capturador Móvil Android:** [`VitalNotifier`](https://github.com/arocutipaluis/VitalNotifier) *(Este repositorio)*
- **Servidor Receptor y ERP Desktop:** [`VitalStock`](https://github.com/arocutipaluis/VitalStock) *(Repositorio complementario)*
**Versión:** 1.2.1  

---

## 1. Propósito del Sistema y Conexión Funcional con `VitalStock`

En las boticas peruanas, el número de cuenta afiliado a **Yape** suele residir en el teléfono móvil del propietario o administrador, mientras que las ventas son atendidas por el personal en las computadoras de caja. Esto ocasionaba demoras para confirmar pagos y vulnerabilidad ante capturas de pantalla falsas.

**VitalNotifier** resuelve este problema actuando como un **agente capturador en segundo plano en Android** que se conecta vía red local Wi-Fi con el sistema de escritorio **[VitalStock](https://github.com/arocutipaluis/VitalStock)**:
1. `VitalNotifier` intercepta en tiempo real las notificaciones del paquete oficial `com.bcp.innovacxion.yapeapp`.
2. Extrae los datos estructurados del pago (`remitente`, `monto`, `fecha_hora` real de emisión, `codigo_seg` y `mensaje_raw`).
3. Almacena localmente el registro en SQLite (`Room`) y lo despacha mediante `HTTP POST http://<IP_PC>:5005/yape` a todas las computadoras de la botica que ejecutan **VitalStock**.

---

## 2. Requerimientos Funcionales (RF)

| Código | Historia de Usuario / Requerimiento Funcional | Componente que lo implementa | Estado |
| :--- | :--- | :--- | :--- |
| **RF-01** | **Escucha Reactiva de Notificaciones Yape (Simples y Agrupadas)**: Interceptar notificaciones de `com.bcp.innovacxion.yapeapp` extrayendo `EXTRA_TEXT`, `EXTRA_BIG_TEXT` y `EXTRA_TEXT_LINES` con ventana de deduplicación para evitar envíos repetidos. | `service/YapeNotificationListenerService.kt` | ✅ Cumplido |
| **RF-02** | **Extracción por Expresiones Regulares (`YapeNotificationParser`)**: Identificar el nombre del remitente, monto en soles (`S/`), código de seguridad y timestamp exacto (`dd/MM/yyyy HH:mm:ss`), descartando notificaciones promocionales que no sean abonos reales. | `parser/YapeNotificationParser.kt` | ✅ Cumplido |
| **RF-03** | **Cola Persistente Offline en SQLite (`Room`)**: Guardar cada pago capturado con estados `PENDING`, `SENT` o `FAILED`, permitiendo reintentar el envío individual o masivo cuando el teléfono regrese a la red Wi-Fi de la botica sin alterar la `fecha_hora` original del Yape. | `data/local/AppDatabase.kt`, `data/local/PaymentDao.kt` | ✅ Cumplido |
| **RF-04** | **Despacho Concurrente Multi-PC**: Permitir registrar múltiples computadoras de destino (ej. *PC Caja 1*, *PC Caja 2*, *PC Administración*) y despachar cada notificación de pago en paralelo a todas las conexiones activas. | `data/model/ServerConnection.kt`, `service/PaymentDispatcher.kt`, `ui/components/EditConnectionDialog.kt` | ✅ Cumplido |
| **RF-05** | **Verificación de Conectividad (`GET /ping`) y Seguridad en Producción**: Incluir prueba de conexión en vivo contra cada PC y eliminar botones de simulación ficticia en producción para garantizar que el 100% de registros en la base de datos de `VitalStock` correspondan a transacciones reales. | `data/remote/VitalStockClient.kt`, `ui/MainActivity.kt` | ✅ Cumplido |

---

## 3. Requerimientos No Funcionales (RNF) y Principios de Diseño

1. **RNF-01 Arquitectura MVVM + Clean Data Layer**: Separación estricta entre `ui/` (Jetpack Compose + `MainViewModel`), `service/` (servicios Android en segundo plano), `parser/` (lógica pura de dominio testeable sin emulador) y `data/` (repositorios locales Room y cliente HTTP OkHttp).
2. **RNF-02 Concurrencia Asíncrona con Kotlin Coroutines**: Todas las operaciones de red (`OkHttp`) y base de datos (`Room`) se ejecutan en `Dispatchers.IO` mediante `SupervisorJob`, garantizando que la interfaz gráfica jamás se bloquee (0 ANRs).
3. **RNF-03 Tolerancia a Fallos de Red**: Timeouts acotados de conexión/lectura en `VitalStockClient` y persistencia previa al envío en `PaymentDispatcher` para que ningún pago se pierda si una PC está apagada.
