// Field Analytics: SOIL-01 history crossing the 30% DRY threshold (JDBC persistence
// proof), plus the fault log. Reads /api/history and /api/faults.
(function () {
  const $ = (id) => document.getElementById(id);
  const DASH = '—';
  const DRY_THRESHOLD = 30; // % - only meaningful for the soil-moisture channel
  const W = 640, H = 240, PAD = 18;

  function timeOnly(ts) {
    return ts && ts.length >= 19 ? ts.slice(11) : (ts || DASH);
  }

  async function refreshChart() {
    const sensorId = $('history-select').value;
    const data = await apiClient.get('/api/history?sensor=' + encodeURIComponent(sensorId) + '&limit=200');
    const rows = data.history || [];
    $('chart-channel').textContent = sensorId;

    const line = $('chart-line');
    const area = $('chart-area');
    const dryGroup = $('chart-dry-points');
    dryGroup.innerHTML = '';
    const isSoil = sensorId === 'SOIL-01';

    if (rows.length === 0) {
      line.setAttribute('points', '');
      area.setAttribute('points', '');
      ['stat-current', 'stat-min', 'stat-max', 'stat-mean'].forEach((id) => { $(id).textContent = DASH; });
      $('stat-rows').textContent = '0';
      $('axis-oldest').textContent = DASH; $('axis-mid').textContent = DASH; $('axis-now').textContent = 'NOW: ' + DASH;
      $('threshold-line').style.display = 'none';
      $('threshold-label').style.display = 'none';
      return;
    }

    const values = rows.map((r) => r.value);
    const unit = rows[rows.length - 1].unit || '';
    let lo = Math.min.apply(null, values);
    let hi = Math.max.apply(null, values);
    if (isSoil) { lo = Math.min(lo, DRY_THRESHOLD); hi = Math.max(hi, DRY_THRESHOLD); }
    if (lo === hi) { lo -= 1; hi += 1; }

    const n = rows.length;
    const x = (i) => (n === 1 ? W / 2 : (i / (n - 1)) * W);
    const y = (v) => H - PAD - ((v - lo) / (hi - lo)) * (H - 2 * PAD);

    const pts = rows.map((r, i) => x(i).toFixed(1) + ',' + y(r.value).toFixed(1));
    line.setAttribute('points', pts.join(' '));
    area.setAttribute('points', '0,' + H + ' ' + pts.join(' ') + ' ' + W + ',' + H);

    // Threshold line + red markers for readings below it (soil only).
    const tl = $('threshold-line');
    const tlab = $('threshold-label');
    if (isSoil) {
      const ty = y(DRY_THRESHOLD);
      tl.setAttribute('y1', ty); tl.setAttribute('y2', ty); tl.style.display = '';
      tlab.setAttribute('y', (ty - 4).toFixed(1)); tlab.style.display = '';
      rows.forEach((r, i) => {
        if (r.value < DRY_THRESHOLD) {
          const c = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
          c.setAttribute('cx', x(i).toFixed(1)); c.setAttribute('cy', y(r.value).toFixed(1));
          c.setAttribute('r', '2.5'); c.setAttribute('fill', '#ba1a1a');
          dryGroup.appendChild(c);
        }
      });
    } else {
      tl.style.display = 'none'; tlab.style.display = 'none';
    }

    const mean = values.reduce((a, b) => a + b, 0) / n;
    const belowOrWarn = rows.filter((r) => (isSoil ? r.value < DRY_THRESHOLD : r.level !== 'OK')).length;
    $('stat-current').textContent = values[n - 1] + unit;
    $('stat-min').textContent = lo.toFixed(1) + unit;
    $('stat-max').textContent = hi.toFixed(1) + unit;
    $('stat-mean').textContent = mean.toFixed(1) + unit;
    $('stat-rows').textContent = n + ' (' + belowOrWarn + ')';
    $('axis-oldest').textContent = timeOnly(rows[0].recordedAt);
    $('axis-mid').textContent = timeOnly(rows[Math.floor(n / 2)].recordedAt);
    $('axis-now').textContent = 'NOW: ' + values[n - 1] + unit;
  }

  async function refreshFaults() {
    const data = await apiClient.get('/api/faults');
    const body = $('fault-table-body');
    body.innerHTML = '';
    $('fault-total').textContent = data.faults.length + ' logged';
    if (data.faults.length === 0) {
      body.innerHTML = '<tr><td colspan="4" class="py-space-sm px-space-sm text-outline">No faults logged.</td></tr>';
      return;
    }
    data.faults.forEach((f) => {
      const tr = document.createElement('tr');
      tr.className = 'hover:bg-surface-container-low/60 transition-colors';
      tr.innerHTML =
        '<td class="py-space-xs px-space-sm font-telemetry-unit text-telemetry-unit text-on-surface-variant whitespace-nowrap">' + esc(f.faultTime) + '</td>'
        + '<td class="py-space-xs px-space-sm font-label-md text-on-surface">' + esc(f.source) + '</td>'
        + '<td class="py-space-xs px-space-sm text-secondary font-semibold">' + esc(f.exception) + '</td>'
        + '<td class="py-space-xs px-space-sm text-on-surface-variant">' + esc(f.message) + '</td>';
      body.appendChild(tr);
    });
  }

  function esc(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  const guard = (fn) => () => fn().catch((e) => console.error('[analytics]', e.message));
  const refreshAll = () => { guard(refreshChart)(); guard(refreshFaults)(); };
  $('history-select').addEventListener('change', guard(refreshChart));
  refreshAll();
  setInterval(refreshAll, 4000);
})();
