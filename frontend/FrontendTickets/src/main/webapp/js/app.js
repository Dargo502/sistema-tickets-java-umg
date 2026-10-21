// ========================================================
/* PROTECCIÓN CONTRA DOBLE ENVÍO + INDICADOR "PROCESANDO"    */
// ========================================================
// Las acciones viajan por VPN a varios servidores y pueden tardar unos segundos. Sin un indicador
// el usuario vuelve a hacer clic y la acción se envía dos veces. Aquí se bloquea el segundo clic.

let envioEnCurso = false;

/** Cubre la pantalla con un velo y un indicador mientras el servidor procesa la acción. */
function mostrarProcesando() {
    if (document.getElementById("velo-procesando")) { return; }
    const velo = document.createElement("div");
    velo.id = "velo-procesando";
    velo.className = "position-fixed top-0 start-0 w-100 h-100 d-flex flex-column align-items-center justify-content-center bg-dark bg-opacity-75 text-white";
    velo.style.zIndex = "2000";
    velo.setAttribute("role", "status");
    velo.innerHTML = '<div class="spinner-border mb-3" aria-hidden="true"></div><div class="fw-semibold">Procesando…</div>';
    document.body.appendChild(velo);
}

function quitarProcesando() {
    document.getElementById("velo-procesando")?.remove();
    document.querySelectorAll("button[disabled][data-bloqueado]").forEach(b => {
        b.disabled = false;
        b.removeAttribute("data-bloqueado");
    });
    envioEnCurso = false;
}

// Todo formulario POST: un solo envío, botones bloqueados y velo de "Procesando"
document.addEventListener("submit", function (e) {
    const form = e.target;
    if (!form || form.method.toLowerCase() !== "post") { return; }
    if (e.defaultPrevented) { return; }                 // otro escuchador lo canceló (p. ej. un confirm)
    if (envioEnCurso) { e.preventDefault(); return; }   // segundo envío: se ignora
    envioEnCurso = true;
    // setTimeout 0: el navegador ya tomó los datos del formulario antes de bloquear los botones
    setTimeout(() => {
        form.querySelectorAll("button[type=submit], button:not([type])").forEach(b => {
            b.disabled = true;
            b.setAttribute("data-bloqueado", "1");
        });
        mostrarProcesando();
    }, 0);
});

// Al volver con el botón "Atrás" el navegador puede restaurar la página bloqueada: se libera
window.addEventListener("pageshow", function (e) {
    if (e.persisted) { quitarProcesando(); }
});

/**
 * Muestra un aviso (sin recargar) encima del tablero. tipo: 'ok' | 'error'
 */
function mostrarAviso(tipo, texto) {
    const cont = document.getElementById("avisoTablero");
    if (!cont) { alert(texto); return; }
    const caja = document.createElement("div");
    caja.className = "alert " + (tipo === "ok" ? "alert-success" : "alert-danger") + " alert-dismissible fade show";
    caja.setAttribute("role", "alert");
    const span = document.createElement("span");
    span.textContent = texto;                       // textContent: nunca interpreta HTML
    const cerrar = document.createElement("button");
    cerrar.type = "button";
    cerrar.className = "btn-close";
    cerrar.setAttribute("data-bs-dismiss", "alert");
    cerrar.setAttribute("aria-label", "Cerrar");
    caja.appendChild(span);
    caja.appendChild(cerrar);
    cont.replaceChildren(caja);
}

/**
 * Envía una acción al servlet (POST) con un formulario oculto. El servlet guarda por SOAP,
 * deja un mensaje y redirige a /inicio, que vuelve a dibujar el tablero con los datos reales.
 */
function enviarAccion(campos) {
    if (envioEnCurso) { return; }
    envioEnCurso = true;
    mostrarProcesando();
    const tablero = document.getElementById("tablero");
    const form = document.createElement("form");
    form.method = "POST";
    form.action = tablero ? tablero.dataset.url : window.location.pathname;
    Object.keys(campos).forEach(nombre => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = nombre;
        input.value = campos[nombre];
        form.appendChild(input);
    });
    document.body.appendChild(form);
    form.submit();
}

/**
 * Pide mover una tarjeta al estado indicado. Si el ticket aún no tiene especialista,
 * abre primero el diálogo "Atender" (reclama y avanza en una sola operación).
 */
function solicitarAvance(tarjeta, idEstadoDestino) {
    const id = tarjeta.dataset.id;
    if (!id || !idEstadoDestino) {
        mostrarAviso("error", "Este ticket no se puede mover (falta su ID o ya está en la última etapa).");
        return;
    }
    if (tarjeta.dataset.puedeReclamar === "true") {
        document.getElementById("atenderIdTicket").value = id;
        document.getElementById("atenderIdEstado").value = idEstadoDestino;
        bootstrap.Modal.getOrCreateInstance(document.getElementById("modalAtender")).show();
        return;
    }
    enviarAccion({ accion: "avanzar", idTicket: id, idNuevoEstado: idEstadoDestino });
}

/** Botón "Atender" / "Pasar a ..." de la tarjeta. */
function avanzarDesdeBoton(boton) {
    const tarjeta = boton.closest(".tarjeta");
    solicitarAvance(tarjeta, tarjeta.dataset.siguienteId);
}

/**
 * Cuenta las tarjetas de cada columna y actualiza el globo numérico de su encabezado.
 */
function actualizarContadoresTablero() {
    document.querySelectorAll(".tablero .columna").forEach(columna => {
        const badge = columna.querySelector(".contador");
        if (badge) {
            badge.textContent = columna.querySelectorAll(".tarjeta").length;
        }
    });
}

/**
 * Oculta de la vista las tarjetas de la última columna (Finalizado).
 * Solo afecta a la pantalla: los tickets siguen guardados en la base de datos.
 */
function limpiarColumnaFinalizados() {
    const columnas = document.querySelectorAll(".tablero .columna");
    if (columnas.length === 0) { return; }
    const ultima = columnas[columnas.length - 1];
    ultima.querySelectorAll(".tarjeta").forEach(tarjeta => {
        tarjeta.style.transition = "all 0.3s ease";
        tarjeta.style.opacity = "0";
        setTimeout(() => { tarjeta.style.display = "none"; }, 300);
    });
}

/**
 * Cambia el tema visual entre Light y Dark Mode guardando 
 * la preferencia del usuario en el navegador de forma local.
 */
function alternarModoOscuro() {
    const raiz = document.documentElement;   // el tema vive SIEMPRE en <html data-theme>
    if (raiz.getAttribute("data-theme") === "dark") {
        raiz.removeAttribute("data-theme");
        localStorage.setItem("temaSeleccionado", "light");
    } else {
        raiz.setAttribute("data-theme", "dark");
        localStorage.setItem("temaSeleccionado", "dark");
    }
    // El cambio de icono (luna/sol) lo hace estilos.css según data-theme.
}

// Al cargar: aplica el tema guardado (el header ya lo aplicó antes de pintar) y recalcula contadores
document.addEventListener("DOMContentLoaded", () => {
    if (localStorage.getItem("temaSeleccionado") === "dark") {
        document.documentElement.setAttribute("data-theme", "dark");
    }
    if (typeof actualizarContadoresTablero === "function") {
        actualizarContadoresTablero();
    }
});


/**
 * Expande la interfaz web para ocupar el 100% del monitor físico.
 */
function togglePantallaCompleta() {
    if (!document.fullscreenElement) {
        document.documentElement.requestFullscreen().catch(err => {
            console.error(`Error al intentar activar pantalla completa: ${err.message}`);
        });
    } else {
        document.exitFullscreen();
    }
}

/**
 * Filtra en tiempo real las tarjetas basándose en el texto escrito en el buscador.
 */
function filtrarTicketsPorTexto() {
    const textoBuscado = document.getElementById("buscadorTickets").value.toLowerCase();
    const tarjetas = document.querySelectorAll(".tarjeta");
    
    tarjetas.forEach(tarjeta => {
        const titulo = tarjeta.querySelector(".titulo")?.textContent.toLowerCase() || "";
        const desc = tarjeta.querySelector(".meta")?.textContent.toLowerCase() || "";
        
        if (titulo.includes(textoBuscado) || desc.includes(textoBuscado)) {
            tarjeta.style.display = ""; 
        } else {
            tarjeta.style.display = "none"; 
        }
    });
}

/**
 * Filtra las tarjetas visibles en el tablero basándose en su nivel de prioridad.
 * @param {string} prioridad - 'alta', 'media', 'baja' o 'todas'
 */
function filtrarPrioridad(prioridad) {
    const tarjetas = document.querySelectorAll(".tarjeta");
    
    tarjetas.forEach(tarjeta => {
        if (prioridad === 'todas') {
            tarjeta.style.display = ""; 
            return;
        }
        
        if (tarjeta.classList.contains(`prio-${prioridad}`)) {
            tarjeta.style.display = "";
        } else {
            tarjeta.style.display = "none";
        }
    });
}

// ========================================================
/* LOGICA DE ARRASTRE Y SOLTADO (DRAG AND DROP NATIVO)      */
// ========================================================

/** Guarda el ID de la tarjeta que el usuario empezó a arrastrar. */
function iniciarArrastre(event) {
    event.dataTransfer.setData("text/plain", event.target.id);
    event.target.style.opacity = "0.5";
}

/** Cancela el comportamiento por defecto del navegador para permitir el soltado. */
function permitirSoltar(event) {
    event.preventDefault();
}

/**
 * Procesa la caída de una tarjeta sobre una columna. El flujo es secuencial
 * (Solicitado -> En Proceso -> Finalizado): solo se permite avanzar una etapa.
 * @param {Event} event
 * @param {HTMLElement} columna - la columna destino (this)
 */
function soltarTicket(event, columna) {
    event.preventDefault();
    const tarjeta = document.getElementById(event.dataTransfer.getData("text/plain"));
    if (!tarjeta) { return; }
    tarjeta.style.opacity = "1";

    const ordenDestino = parseInt(columna.dataset.orden, 10);
    const ordenOrigen = parseInt(tarjeta.dataset.orden, 10);
    if (ordenDestino === ordenOrigen) { return; }
    if (ordenDestino !== ordenOrigen + 1) {
        mostrarAviso("error", "Solo se puede avanzar una etapa a la vez; el flujo es secuencial y no admite retroceso.");
        return;
    }
    solicitarAvance(tarjeta, columna.dataset.estadoId);
}

// Vinculamos un escuchador de seguridad por si el usuario cancela el arrastre a mitad de camino
document.addEventListener("dragend", (event) => {
    if (event.target && event.target.style) {
        event.target.style.opacity = "1";
    }
});

// Lógica para encender el Highlight correcto en el menú según la URL actual de tu navegador
document.addEventListener("DOMContentLoaded", () => {
    const rutaActual = window.location.pathname;
    
    // Removemos cualquier brillo viejo por seguridad
    document.querySelectorAll("#mainMenuNav .nav-link").forEach(enlace => {
        enlace.classList.remove("active");
    });
    
    // Evaluamos en qué Servlet se encuentra parado el usuario y encendemos el ID correspondiente
    if (rutaActual.includes("/inicio")) {
        document.getElementById("nav-inicio")?.classList.add("active");
    } else if (rutaActual.includes("/historial")) {
        document.getElementById("nav-historial")?.classList.add("active");
    } else if (rutaActual.includes("/notificaciones")) {
        document.getElementById("nav-notificaciones")?.classList.add("active");
    } else if (rutaActual.includes("/config")) {
        document.getElementById("nav-config")?.classList.add("active");
    }
    
    // Ejecutamos el conteo inicial de tus tarjetas Kanban
    if (typeof actualizarContadoresTablero === "function") {
        actualizarContadoresTablero();
    }
});

