// Runs on every authenticated page (dashboard, hardware). Guards the page, fills the
// header identity, gates the Hardware tab by role, injects the always-on SIMULATED
// badge and the live clock, and wires logout.
(function () {
  const session = SessionStore.get();
  if (!session) {
    window.location.href = 'login.html';
    return;
  }

  // --- header identity ---
  const usernameEl = document.getElementById('header-username');
  const roleEl = document.getElementById('header-role');
  if (usernameEl) usernameEl.textContent = 'user: ' + session.username;
  if (roleEl) roleEl.textContent = session.role === 'TECHNICIAN' ? 'Technician' : 'Farmer';

  // --- role gating: only a Technician (canAccessHardware) sees the Hardware tab ---
  const hardwareLink = document.querySelector('a[data-path="hardware"]');
  if (hardwareLink && !session.canAccessHardware) {
    hardwareLink.style.display = 'none';
  }

  // --- persistent SIMULATED DATA badge, injected into the header so every page shows it ---
  const header = document.querySelector('header');
  if (header && !document.getElementById('simulated-badge')) {
    const badge = document.createElement('div');
    badge.id = 'simulated-badge';
    badge.title = 'All sensor data is simulated - there is no physical hardware';
    // Position/z via inline style (arbitrary Tailwind utilities aren't in the subset build);
    // colour + typography via known compiled classes.
    badge.style.cssText = 'position:fixed;top:8px;right:16px;z-index:60;';
    badge.className = 'flex items-center gap-space-2xs px-space-sm py-space-2xs rounded '
      + 'bg-secondary-fixed text-on-secondary-fixed shadow-sm';
    badge.innerHTML =
      '<span class="material-symbols-outlined" style="font-size:14px">science</span>'
      + '<span class="font-label-caps text-label-caps font-bold uppercase tracking-wider">Simulated Data</span>';
    document.body.appendChild(badge);
  }

  // --- live clock in the header (replaces the old hardcoded time) ---
  const clockEl = document.getElementById('header-clock');
  if (clockEl) {
    const tick = () => {
      clockEl.textContent = new Date().toLocaleTimeString([], { hour12: false });
    };
    tick();
    setInterval(tick, 1000);
  }

  // --- logout ---
  const logoutLink = document.getElementById('logout-link');
  if (logoutLink) {
    logoutLink.addEventListener('click', function (e) {
      e.preventDefault();
      apiClient.post('/api/logout').catch(() => {}).finally(() => {
        SessionStore.clear();
        window.location.href = 'login.html';
      });
    });
  }
})();
