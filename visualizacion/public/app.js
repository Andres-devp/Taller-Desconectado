// Visor de mapas de calor de afectación por desastres.
// Consume la API central (/api/v1/...). Por defecto usa el mismo origen (proxy
// de nginx); se puede apuntar a otra API con ?api=http://host:8000
(function () {
  "use strict";

  const API = (new URLSearchParams(location.search).get("api") || "").replace(/\/$/, "");
  const REFRESCO_MS = 30000;
  const COLORES = { 1: "#2e7d32", 2: "#9e9d24", 3: "#f9a825", 4: "#ef6c00", 5: "#c62828" };
  const ETIQUETAS = { 1: "1 · Leve", 2: "2 · Moderado", 3: "3 · Serio", 4: "4 · Grave", 5: "5 · Crítico" };

  const $ = (id) => document.getElementById(id);
  const ui = {
    estado: $("estado"), evento: $("f-evento"), nivel: $("f-nivel"), desde: $("f-desde"),
    hasta: $("f-hasta"), puntos: $("f-puntos"), celda: $("f-celda"),
    total: $("total"), niveles: $("niveles"), zonas: $("zonas"),
  };

  // ---------- Mapa ----------
  const mapa = L.map("mapa", { zoomControl: true }).setView([4.57, -74.3], 6); // Colombia
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap",
  }).addTo(mapa);

  const capaCalor = L.heatLayer([], {
    radius: 28, blur: 22, maxZoom: 12, max: 1.0, minOpacity: 0.35,
    gradient: { 0.2: "#2e7d32", 0.4: "#c0ca33", 0.6: "#fbc02d", 0.8: "#ef6c00", 1.0: "#b71c1c" },
  }).addTo(mapa);
  const capaPuntos = L.layerGroup();
  const capaZona = L.layerGroup().addTo(mapa);

  const leyenda = L.control({ position: "bottomright" });
  leyenda.onAdd = () => {
    const div = L.DomUtil.create("div", "leyenda");
    div.innerHTML = "<strong>Nivel de afectación</strong><br>" +
      [1, 2, 3, 4, 5].map((n) => `<i style="background:${COLORES[n]}"></i>${ETIQUETAS[n]}`).join("<br>");
    return div;
  };
  leyenda.addTo(mapa);

  let primeraCarga = true;

  // ---------- Datos ----------
  function parametros() {
    const p = new URLSearchParams();
    if (ui.evento.value) p.set("evento", ui.evento.value);
    if (ui.nivel.value) p.set("nivel_min", ui.nivel.value);
    if (ui.desde.value) p.set("desde", new Date(ui.desde.value + "T00:00:00").toISOString());
    if (ui.hasta.value) p.set("hasta", new Date(ui.hasta.value + "T23:59:59").toISOString());
    return p;
  }

  async function obtener(ruta, params) {
    const qs = params && params.toString() ? "?" + params : "";
    const r = await fetch(`${API}/api/v1/${ruta}${qs}`, { headers: { Accept: "application/json" } });
    if (!r.ok) throw new Error(`${ruta}: HTTP ${r.status}`);
    return r.json();
  }

  async function cargarEventos() {
    const eventos = await obtener("eventos");
    const actual = ui.evento.value;
    ui.evento.innerHTML = '<option value="">Todos los eventos</option>' +
      eventos.map((e) => `<option value="${escapar(e.evento)}">${escapar(e.evento)} (${e.reportes})</option>`).join("");
    ui.evento.value = eventos.some((e) => e.evento === actual) ? actual : "";
  }

  async function refrescar() {
    try {
      await cargarEventos();
      const p = parametros();
      const pz = new URLSearchParams(p);
      pz.set("celda", ui.celda.value);
      pz.set("top", "10");
      const [calor, reportes, zonas] = await Promise.all([
        obtener("heatmap", p), obtener("reportes", p), obtener("zonas", pz),
      ]);
      pintarCalor(calor);
      pintarPuntos(reportes);
      pintarResumen(reportes);
      pintarZonas(zonas);
      if (primeraCarga && calor.length) {
        mapa.fitBounds(L.latLngBounds(calor.map((c) => [c[0], c[1]])), { padding: [30, 30], maxZoom: 12 });
        primeraCarga = false;
      }
      ui.estado.textContent = "Actualizado " + new Date().toLocaleTimeString();
      ui.estado.classList.remove("error");
    } catch (err) {
      console.error(err);
      ui.estado.textContent = "Sin conexión con el sistema central";
      ui.estado.classList.add("error");
    }
  }

  // ---------- Pintado ----------
  function pintarCalor(puntos) {
    capaCalor.setLatLngs(puntos);
  }

  function pintarPuntos(reportes) {
    capaPuntos.clearLayers();
    reportes.forEach((r) => {
      L.circleMarker([r.latitud, r.longitud], {
        radius: 5 + r.nivel, color: "#fff", weight: 1, fillColor: COLORES[r.nivel], fillOpacity: 0.9,
      }).bindPopup(
        `<strong>${ETIQUETAS[r.nivel]}</strong><br>` +
        (r.evento ? `${escapar(r.evento)}<br>` : "") +
        (r.descripcion ? `<em>${escapar(r.descripcion)}</em><br>` : "") +
        `${r.latitud.toFixed(5)}, ${r.longitud.toFixed(5)}<br>` +
        `Capturado: ${new Date(r.capturado_en).toLocaleString()}`
      ).addTo(capaPuntos);
    });
  }

  function pintarResumen(reportes) {
    ui.total.textContent = reportes.length;
    const conteo = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
    reportes.forEach((r) => { conteo[r.nivel]++; });
    const max = Math.max(1, ...Object.values(conteo));
    ui.niveles.innerHTML = [5, 4, 3, 2, 1].map((n) =>
      `<li><span>${ETIQUETAS[n]}</span>` +
      `<span class="barra-nivel" style="width:${(conteo[n] / max) * 100}%;background:${COLORES[n]}"></span>` +
      `<span class="n">${conteo[n]}</span></li>`).join("");
  }

  function pintarZonas(zonas) {
    if (!zonas.length) {
      ui.zonas.innerHTML = '<p class="vacio">Aún no hay reportes sincronizados.</p>';
      return;
    }
    ui.zonas.innerHTML = zonas.map((z, i) =>
      `<li tabindex="0" data-i="${i}">` +
      `<strong>${z.reportes} reportes</strong> · promedio ${z.nivel_promedio} · máx ${z.nivel_maximo}<br>` +
      `<span class="coord">${z.latitud.toFixed(3)}, ${z.longitud.toFixed(3)}</span></li>`).join("");
    ui.zonas.querySelectorAll("li").forEach((li) => {
      const ir = () => enfocarZona(zonas[Number(li.dataset.i)]);
      li.addEventListener("click", ir);
      li.addEventListener("keydown", (e) => { if (e.key === "Enter") ir(); });
    });
  }

  function enfocarZona(z) {
    const m = z.celda / 2;
    const limites = [[z.latitud - m, z.longitud - m], [z.latitud + m, z.longitud + m]];
    capaZona.clearLayers();
    L.rectangle(limites, { color: COLORES[z.nivel_maximo], weight: 2, fill: false, dashArray: "6 4" }).addTo(capaZona);
    mapa.fitBounds(limites, { padding: [40, 40] });
  }

  function escapar(s) {
    return String(s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
  }

  // ---------- Eventos ----------
  [ui.evento, ui.nivel, ui.desde, ui.hasta, ui.celda].forEach((el) => el.addEventListener("change", refrescar));
  ui.puntos.addEventListener("change", () => {
    if (ui.puntos.checked) capaPuntos.addTo(mapa); else mapa.removeLayer(capaPuntos);
  });

  refrescar();
  setInterval(refrescar, REFRESCO_MS);
})();
