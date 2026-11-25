// Module Bay: base sensors + mission modules, live from /api/sensors and /api/modules.
// Shows the polymorphic module architecture - each module is a card with its state.
(function () {
  const $ = (id) => document.getElementById(id);
  const DASH = '—';

  // Defence in depth: the nav hides the Hardware tab for a Farmer, but if one reaches
  // this URL directly, send them back. (The real authorization is the Technician class.)
  const session = SessionStore.get();
  if (session && !session.canAccessHardware) {
    window.location.href = 'dashboard.html';
    return;
  }

  // Maps an OK/WARNING/CRITICAL/INACTIVE level or a module state to a colour family.
  function colourFor(token) {
    switch (token) {
      case 'OK': case 'ACTIVE': return 'primary';
      case 'WARNING': case 'MOUNTED': case 'SUITABLE': return 'secondary';
      case 'CRITICAL': case 'ERROR': case 'FAULT': return 'error';
      default: return 'outline'; // INACTIVE / unknown
    }
  }

  function pill(label, token) {
    const c = colourFor(token);
    return '<span class="inline-flex items-center gap-space-2xs px-space-sm py-space-2xs rounded bg-surface-container-low '
      + 'font-label-caps text-label-caps font-bold uppercase text-' + c + '">'
      + '<span class="w-1.5 h-1.5 rounded-full bg-' + c + '"></span>' + esc(label) + '</span>';
  }

  function valueRow(valueText) {
    return '<div class="bg-surface-container-low rounded p-space-xs flex items-center justify-between">'
      + '<span class="font-label-caps text-label-caps text-on-surface-variant">LATEST VALUE</span>'
      + '<span class="font-telemetry-readout text-body-sm font-semibold text-on-surface">' + esc(valueText) + '</span></div>';
  }

  async function refresh() {
    const [sensorData, moduleData] = await Promise.all([
      apiClient.get('/api/sensors'),
      apiClient.get('/api/modules'),
    ]);
    renderSensors(sensorData.sensors);
    renderModules(moduleData.modules);
    $('bay-updated').textContent = new Date().toLocaleTimeString([], { hour12: false });
  }

  function renderSensors(sensors) {
    const grid = $('base-sensors-grid');
    grid.innerHTML = '';
    let online = 0;
    sensors.forEach((s) => {
      const isOnline = s.connected && s.level !== 'INACTIVE';
      if (isOnline) online++;
      const valueText = s.value == null ? DASH : s.value + (s.unit ? ' ' + s.unit : '');
      const card = document.createElement('div');
      card.className = 'bg-surface-container-lowest rounded-xl p-space-md shadow-sm flex flex-col gap-space-sm';
      card.innerHTML =
        '<div class="flex items-start justify-between gap-space-xs">'
          + '<div><span class="font-label-caps text-label-caps text-on-surface-variant uppercase tracking-wider block">'
          + esc(s.type) + (s.critical ? ' • CRITICAL' : '') + '</span>'
          + '<h3 class="font-headline-sm text-headline-sm text-on-surface font-semibold">' + esc(s.name) + '</h3>'
          + '<span class="font-label-caps text-label-caps text-outline">' + esc(s.id) + '</span></div>'
          + pill(s.status, s.level) + '</div>'
        + '<div class="font-body-sm text-body-sm text-on-surface-variant">Raw: <span class="font-telemetry-unit">' + esc(s.raw || DASH) + '</span></div>'
        + valueRow(valueText);
      grid.appendChild(card);
    });
    $('sensor-op-count').textContent = online + ' / ' + sensors.length;
  }

  function renderModules(modules) {
    const grid = $('module-cards-grid');
    grid.innerHTML = '';
    let active = 0;
    modules.forEach((m) => {
      if (m.state === 'ACTIVE') active++;
      const valueText = m.value == null ? DASH : m.value + (m.unit ? ' ' + m.unit : '');
      const card = document.createElement('div');
      card.className = 'bg-surface-container-lowest rounded-xl p-space-md shadow-sm flex flex-col gap-space-sm';
      card.innerHTML =
        '<div class="flex items-start justify-between gap-space-xs">'
          + '<div><h3 class="font-headline-sm text-headline-sm text-on-surface font-semibold">' + esc(m.name) + '</h3>'
          + '<span class="font-label-caps text-label-caps text-outline">' + esc(m.id) + '</span></div>'
          + pill(m.state, m.state) + '</div>'
        + '<p class="font-body-sm text-body-sm text-on-surface-variant">' + esc(m.description) + '</p>'
        + valueRow(valueText)
        + '<div class="flex items-center justify-between font-label-caps text-label-caps text-outline">'
          + '<span>MOUNTED</span><span>' + esc(m.mountedAt || DASH) + '</span></div>';
      grid.appendChild(card);
    });
    $('module-count').textContent = modules.length + ' (' + active + ' active)';
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  const guarded = () => refresh().catch((e) => console.error('[module-bay]', e.message));
  guarded();
  setInterval(guarded, 1500);
})();
