// Dashboard live-wiring. Every value on screen comes from the API; a missing value
// renders as an em dash, never a decorative number.
//
// Fast panels (status, sensors) refresh every 1.5s; slower ones (history, faults,
// obstacles, recommendations) every 3s.
(function () {
  const $ = (id) => document.getElementById(id);
  const DASH = '—';

  // Speed is a 0-100 duty cycle; the odometer treats 100% as 0.5 m/s.
  const toMetresPerSecond = (dutyPercent) => (dutyPercent / 100) * 0.5;

  function levelColour(level) {
    switch (level) {
      case 'OK': return 'text-primary';
      case 'WARNING': return 'text-secondary';
      case 'CRITICAL':
      case 'ERROR': return 'text-error';
      default: return 'text-outline'; // INACTIVE / unknown
    }
  }

  // ---------- fast panels: /api/status + /api/sensors ----------

  async function refreshStatus() {
    const s = await apiClient.get('/api/status');
    $('status-badge').textContent = 'STATE: ' + s.status;
    $('stat-battery').textContent = s.battery == null ? DASH : Math.round(s.battery) + '%';
    $('stat-speed').textContent = toMetresPerSecond(s.speed).toFixed(2) + ' m/s';
    $('stat-strategy').textContent = s.strategy || DASH;
    $('stat-distance').textContent = s.distanceMeters == null ? DASH : s.distanceMeters.toFixed(1);
    $('stat-status-desc').textContent = s.statusDescription || DASH;
    $('ctrl-strategy').textContent = s.strategy || DASH;
    $('ctrl-state').textContent = s.status || DASH;
    $('ctrl-speed').textContent = toMetresPerSecond(s.speed).toFixed(2) + ' m/s';
    highlightStrategy(s.strategy);
  }

  function highlightStrategy(active) {
    // Only classes present in the precompiled Tailwind subset are used here.
    document.querySelectorAll('.strategy-btn').forEach((btn) => {
      const isActive = btn.getAttribute('data-strategy') === active;
      btn.classList.toggle('bg-primary-container', isActive);
      btn.classList.toggle('text-on-primary-container', isActive);
      btn.classList.toggle('shadow-md', isActive);
      btn.classList.toggle('bg-surface-container-low', !isActive);
      btn.classList.toggle('text-on-surface', !isActive);
      const dot = btn.querySelector('.strategy-dot');
      if (dot) {
        dot.classList.toggle('bg-on-primary-container', isActive);
        dot.classList.toggle('bg-surface-container-highest', !isActive);
      }
    });
  }

  async function refreshSensors() {
    const data = await apiClient.get('/api/sensors');
    const body = $('sensor-table-body');
    body.innerHTML = '';
    data.sensors.forEach((sensor) => {
      const value = sensor.value == null ? DASH : sensor.value + (sensor.unit ? ' ' + sensor.unit : '');
      const raw = sensor.raw && sensor.raw.length ? sensor.raw : sensor.type;
      const tr = document.createElement('tr');
      tr.className = 'hover:bg-surface-container-low/60 transition-colors';
      tr.innerHTML =
        '<td class="py-space-xs px-space-sm">'
          + '<div class="font-body-md text-on-surface font-medium">' + esc(sensor.name) + '</div>'
          + '<div class="font-label-caps text-label-caps text-outline">' + esc(sensor.id)
          + (sensor.critical ? ' • CRITICAL' : '') + '</div></td>'
        + '<td class="py-space-xs px-space-sm font-telemetry-unit text-telemetry-unit text-on-surface-variant">' + esc(raw) + '</td>'
        + '<td class="py-space-xs px-space-sm text-right font-telemetry-readout text-telemetry-unit text-on-surface font-semibold">' + esc(value) + '</td>'
        + '<td class="py-space-xs px-space-sm text-right"><span class="font-label-caps text-label-caps font-bold ' + levelColour(sensor.level) + '">' + esc(sensor.status) + '</span></td>';
      body.appendChild(tr);
    });
    // Camera status mirrors the sensor table (always INACTIVE in simulation).
    const cam = data.sensors.find((x) => x.type === 'Camera');
    if (cam) $('camera-status').textContent = cam.status === 'INACTIVE' ? 'FEED OFFLINE' : cam.status;
  }

  // ---------- slow panels ----------

  async function refreshFaults() {
    const data = await apiClient.get('/api/faults');
    const list = $('fault-list');
    list.innerHTML = '';
    $('fault-count').textContent = data.faults.length + ' Logged Alerts';
    $('fault-updated').textContent = 'UPDATED ' + new Date().toLocaleTimeString([], { hour12: false });
    if (data.faults.length === 0) {
      list.innerHTML = '<div class="font-body-sm text-body-sm text-outline">No faults logged.</div>';
      return;
    }
    data.faults.forEach((f) => {
      const row = document.createElement('div');
      row.className = 'flex items-start gap-space-xs p-space-xs rounded bg-surface-container-low/70';
      row.innerHTML =
        '<span class="material-symbols-outlined text-[16px] text-secondary mt-0.5">warning</span>'
        + '<div class="flex flex-col"><div class="font-label-md text-label-md text-on-surface font-semibold">'
        + esc(f.exception) + ' <span class="text-outline font-normal">· ' + esc(f.source) + '</span></div>'
        + '<div class="font-body-sm text-body-sm text-on-surface-variant">' + esc(f.message) + '</div>'
        + '<div class="font-label-caps text-label-caps text-outline">' + esc(f.faultTime) + '</div></div>';
      list.appendChild(row);
    });
  }

  async function refreshObstacles() {
    const o = await apiClient.get('/api/obstacles');
    $('stat-obstacle-detected').textContent = o.detected;
    $('stat-obstacle-collisions').textContent = o.undetected;
    $('stat-obstacle-total').textContent = o.total;
  }

  async function refreshRecommendations() {
    const data = await apiClient.get('/api/recommendations');
    const wrap = $('recommendations-list');
    wrap.innerHTML = '';
    if (data.recommendations.length === 0) {
      wrap.innerHTML =
        '<div class="md:col-span-3 flex items-center gap-space-sm p-space-md rounded-lg bg-surface-container-low">'
        + '<span class="material-symbols-outlined text-[20px] text-primary">check_circle</span>'
        + '<span class="font-body-md text-on-surface-variant">No active advisories — all readings within normal range.</span></div>';
      return;
    }
    const sevColour = { INFO: 'text-primary', WARNING: 'text-secondary', CRITICAL: 'text-error' };
    data.recommendations.forEach((r) => {
      const card = document.createElement('div');
      card.className = 'bg-surface-container-low rounded-lg p-space-md flex flex-col gap-space-xs';
      card.innerHTML =
        '<div class="flex items-center justify-between"><span class="font-label-caps text-label-caps font-bold uppercase '
        + (sevColour[r.severity] || 'text-primary') + '">' + esc(r.severity) + '</span>'
        + '<span class="font-label-caps text-label-caps text-outline">' + esc(r.ruleId) + '</span></div>'
        + '<div class="font-headline-sm text-label-md text-on-surface font-bold">' + esc(r.title) + '</div>'
        + '<div class="font-body-sm text-body-sm text-on-surface-variant">' + esc(r.message) + '</div>'
        + '<div class="mt-space-2xs flex items-start gap-space-2xs"><span class="material-symbols-outlined text-[16px] text-primary">arrow_forward</span>'
        + '<span class="font-body-sm text-body-sm text-on-surface">' + esc(r.action) + '</span></div>';
      wrap.appendChild(card);
    });
  }

  // ---------- live history chart ----------

  async function refreshHistory() {
    const sensorId = $('history-select').value;
    const data = await apiClient.get('/api/history?sensor=' + encodeURIComponent(sensorId) + '&limit=60');
    const rows = data.history || [];
    const line = $('history-line');
    const area = $('history-area');
    if (rows.length === 0) {
      line.setAttribute('points', '');
      area.setAttribute('points', '');
      $('history-channel').textContent = 'CHANNEL: ' + sensorId;
      $('history-stats').textContent = 'no data';
      $('history-now').textContent = 'NOW: ' + DASH;
      $('history-oldest').textContent = DASH;
      return;
    }
    const W = 360, H = 140, PAD = 12;
    const values = rows.map((r) => r.value);
    let min = Math.min.apply(null, values);
    let max = Math.max.apply(null, values);
    if (min === max) { min -= 1; max += 1; } // avoid divide-by-zero on a flat line
    const n = rows.length;
    const x = (i) => (n === 1 ? W / 2 : (i / (n - 1)) * W);
    const y = (v) => H - PAD - ((v - min) / (max - min)) * (H - 2 * PAD);
    const pts = rows.map((r, i) => x(i).toFixed(1) + ',' + y(r.value).toFixed(1));
    line.setAttribute('points', pts.join(' '));
    area.setAttribute('points', '0,' + H + ' ' + pts.join(' ') + ' ' + W + ',' + H);

    const unit = rows[rows.length - 1].unit || '';
    const mean = values.reduce((a, b) => a + b, 0) / n;
    $('history-channel').textContent = 'CHANNEL: ' + sensorId;
    $('history-stats').textContent = 'MEAN: ' + mean.toFixed(1) + unit + ' • n=' + n;
    $('history-now').textContent = 'NOW: ' + values[values.length - 1] + unit;
    $('history-oldest').textContent = rows[0].recordedAt ? rows[0].recordedAt.slice(11) : DASH;
  }

  // ---------- strategy switching ----------

  function wireStrategyButtons() {
    document.querySelectorAll('[data-strategy]').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const strategy = btn.getAttribute('data-strategy');
        const msg = $('strategy-msg');
        try {
          const res = await apiClient.post('/api/strategy', { strategy });
          msg.classList.add('hidden');
          highlightStrategy(res.strategy);
          refreshStatus();
        } catch (err) {
          // Manual returns a clean 400 by design: show it as a note, not an error.
          msg.textContent = err.message;
          msg.classList.remove('hidden');
        }
      });
    });
  }

  // ---------- polling loops ----------

  function guarded(fn) {
    return () => fn().catch((e) => console.error('[dashboard] ' + fn.name + ':', e.message));
  }

  function start() {
    wireStrategyButtons();
    $('history-select').addEventListener('change', guarded(refreshHistory));
    $('btn-tick').addEventListener('click', () => { fast(); slow(); });

    const fast = () => { guarded(refreshStatus)(); guarded(refreshSensors)(); };
    const slow = () => {
      guarded(refreshFaults)();
      guarded(refreshObstacles)();
      guarded(refreshRecommendations)();
      guarded(refreshHistory)();
    };
    fast();
    slow();
    setInterval(fast, 1500);
    setInterval(slow, 3000);
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  start();
})();
