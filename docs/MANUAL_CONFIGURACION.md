# Manual de Configuración y Despliegue — Sistema de Gestión de Tickets

Este manual detalla los pasos requeridos para el aprovisionamiento, configuración de red y despliegue del ecosistema del sistema distribuido de tres capas en un entorno de red local aislada o virtualizada mediante Radmin VPN.

## 1. Arquitectura de Despliegue Real

El sistema está diseñado bajo una arquitectura distribuida desacoplada, donde la capa de presentación (web) consume las reglas de negocio exclusivamente mediante servicios SOAP.

| Servidor / Nodo | Software Base | Puerto | Componente / Artefacto |
| :--- | :--- | :--- | :--- |
| **Base de Datos** | Microsoft SQL Server Express | 1433 | Base de datos `DB_ControlTickets` |
| **Servicios Backend** | GlassFish Server 7 + JDK 17 | 8080 | Servicio SOAP `BackendTickets.war` |
| **Aplicación Web** | GlassFish Server 7 (o Tomcat 10.1) | 8080 | Aplicación MVC `FrontendTickets.war` |
| **Cliente Final** | Navegador de Dispositivo Móvil | — | Interfaz de usuario vía HTTP |

---

## 2. Requisitos de Entorno y Dependencias

Para el correcto ensamblado del proyecto, asegúrese de contar con las siguientes especificaciones técnicas:
* **Java Development Kit (JDK):** Versión 17 (arquitectura Jakarta EE 10).
* **Servidor de Aplicaciones:** GlassFish 7.x (soporte para Servlets 6.0 y JAX-WS).
* **Gestor de Dependencias:** Maven 3.9+ (requerido para compilar la capa web).
* **Motor de Base de Datos:** SQL Server 2019 o superior + SQL Server Management Studio (SSMS).
* **Controlador JDBC:** `mssql-jdbc-13.4.0.jre11.jar` (provisto localmente dentro de la ruta `backend/BackendTickets/lib/`).

---

## 3. Configuración de la Red Distribuida

El correcto funcionamiento de las llamadas RPC/SOAP y las consultas JDBC depende del enrutamiento sin restricciones entre nodos. 

### Parámetros de Red Asignados
* **Segmento de Red Local (Radmin VPN):** `26.0.0.0/8`
* **Nodo Base de Datos (PC Libbny):** `26.140.214.229`
* **Nodo de Servicios (PC Richard):** `26.247.182.199`
* **Nodo Web Frontend (Tu PC):** `26.x.x.x` (IP dinámica asignada por el adaptador de Radmin).
* **Máscara de Subred:** `255.0.0.0`
* **Perfil de Red en Windows:** Debe establecerse explícitamente como **Red Privada** en los tres equipos para permitir la visibilidad de sockets.
* **Aislamiento de Clientes (AP Isolation):** Desactivado en el enrutador físico si las pruebas se realizan de manera local con Wi-Fi.

---

## 4. Configuración del Servidor de Base de Datos (Nodo BD)

1. **Creación del Esquema:** Abra SQL Server Management Studio (SSMS) en el nodo correspondiente, cargue el script ubicado en `database/control_tickets_db.sql` y ejecútelo para inicializar las estructuras, catálogos bases, procedimientos almacenados y los *triggers* de control FIFO (`TR_Ticket_Insertar`, `TR_Ticket_ValidarEstado`, `TR_Ticket_Reclamo`).
2. **Seguridad del Motor:** Habilite el **Modo de Autenticación Mixta** (SQL Server y Windows) en las propiedades del servidor. Ejecute `database/01_crear_usuario_app.sql` para crear el inicio de sesión `tickets_app`, de uso exclusivo del backend (no se usa `sa`).
3. **Activación de Sockets TCP/IP:**
   * Abra el *SQL Server Configuration Manager*.
   * Vaya a *Configuración de red de SQL Server* -> *Protocolos de SQLEXPRESS*.
   * Habilite el protocolo **TCP/IP**.
   * Ingrese a las propiedades de TCP/IP, diríjase a la pestaña *Direcciones IP*, desplácese hasta la sección **IPAll**, limpie los *Puertos TCP Dinámicos* y asigne en *Puerto TCP* el valor fijo `1433`.
   * Reinicie el servicio de SQL Server (`SQLEXPRESS`).
4. **Apertura del Cortafuegos de Windows:** Dado que el adaptador virtual trata la conexión como red pública inicial, ejecute una consola de comandos (`cmd`) con privilegios de Administrador y habilite la regla de entrada con la siguiente instrucción de red:
   ```bash
   netsh advfirewall firewall add rule name="SQL Server 1433" dir=in action=allow protocol=TCP localport=1433
   ```

---

## 5. Configuración del Servidor de Servicios (Nodo Backend)

1. **Parametrización de la Conexión JDBC:** La conexión de los 9 servicios está centralizada en `servicios.Conexion`. Copie `backend/BackendTickets/src/java/db.properties.example` como `db.properties` en la misma carpeta y ajuste la IP del nodo de base de datos (puerto fijo 1433), el usuario y la contraseña:
   ```properties
   db.url=jdbc:sqlserver://IP-BD:1433;databaseName=DB_ControlTickets;encrypt=true;trustServerCertificate=true;
   db.user=tickets_app
   db.password=su-contraseña
   ```
   `db.properties` está excluido del control de versiones para que las credenciales no se publiquen.
2. **Compilación y Despliegue:** Ejecute una limpieza y compilación (*Clean and Build*) para empaquetar el artefacto `BackendTickets.war`. Acceda a la consola de administración de GlassFish (`http://localhost:4848`), navegue a *Applications* -> *Deploy*, cargue el archivo comprimido y confirme la publicación del contexto.
3. **Apertura de Firewall:** Asegure la accesibilidad del puerto `8080` de manera externa para la escucha de peticiones XML entrantes.
4. **Verificación del Contrato WSDL:** Valide el estado de publicación abriendo el navegador del nodo web e ingresando a la URL del descriptor principal:
   ```http
   http://IP-SERVICIOS:8080/BackendTickets/WSTickets?wsdl
   ```
   *Nota: Verifique que figuren expuestos los 9 endpoints requeridos por el core (`WSEmpresas`, `WSRoles`, `WSUsuarios`, `WSCategorias`, `WSEstados`, `WSPrioridades`, `WSTickets`, `WSHistorial`, `WSNotificaciones`).*

---

## 6. Configuración de la Aplicación Web (Nodo Frontend)

1. **Construcción con Maven:** En la terminal local de desarrollo, compile el empaquetado del frontend web mediante el pom del proyecto:
   ```bash
   mvn clean package
   ```
   *(Si el despliegue de contingencia se realiza sobre Tomcat/XAMPP, use el perfil de inyección de descriptores JSTL: `mvn clean package -Ptomcat`).*
2. **Despliegue de la Presentación:** Copie el archivo resultante `target/FrontendTickets.war` y despliéguelo sobre el contenedor GlassFish del entorno local.
3. **Resolución de la URL SOAP.** Al arrancar, la aplicación toma la URL base en este orden de prioridad:
   * **1. Parámetro JVM:** `-Dsoap.base.url=http://IP-SERVICIOS:8080/BackendTickets`
   * **2. Variable de entorno:** `TICKETS_SOAP_URL`
   * **3. Archivo `config.properties`:** propiedad `soap.base.url`
   * **4. Valor por defecto:** `http://localhost:8080/BackendTickets`

   Una vez en ejecución, la URL puede cambiarse en caliente desde la pantalla **Configuración** (`/config`) sin reiniciar; el cambio se conserva en memoria hasta el siguiente reinicio del servidor.
4. **Test de Conectividad:** Ingrese al panel de configuración del frontend y presione el botón **"Probar conexión con los servicios"**. Confirme que el diagnóstico de los 9 descriptores retorne el estado de verificación exitosa (**OK**).

---

## 7. Pruebas y Consumo desde Dispositivos Móviles

1. **Enlace a la Red Común:** Conecte el terminal móvil al mismo punto de acceso inalámbrico (Wi-Fi) unificado de las terminales de desarrollo.
2. **Asignación de IP:** Configure el dispositivo con direccionamiento IP estático o realice una reserva de concesión DHCP para evitar colisiones dentro del segmento del laboratorio.
3. **Acceso a la Plataforma:** Inicie el explorador del dispositivo móvil e invoque la URL del nodo web para operar el tablero Kanban interactivo y el registro de incidencias:
   ```http
   http://[IP_TU_PC_FRONTEND]:8080/FrontendTickets/
   ```
