class AuthView {
  constructor() {
    this.loginForm = document.getElementById('loginForm');
    this.registerForm = document.getElementById('register-form');
    if (this.loginForm) this.bindLogin();
    if (this.registerForm) this.bindRegister();
  }

  bindLogin() {
    this.loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const username = document.getElementById('operatorId').value.trim();
      const password = document.getElementById('passwordField').value;
      try {
        const data = await apiClient.post('/api/login', { username, password });
        SessionStore.set({
          token: data.token,
          username: data.username,
          role: data.role,
          canAccessHardware: data.canAccessHardware === true,
        });
        window.location.href = 'dashboard.html';
      } catch (err) {
        this.showError('authAlert', 'authAlertMessage', err.message);
      }
    });
  }

  bindRegister() {
    this.registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const username = document.getElementById('username').value.trim();
      const password = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirm-password').value;
      const roleInput = document.querySelector('input[name="role"]:checked');
      const role = roleInput ? roleInput.value.toUpperCase() : 'FARMER';

      if (password !== confirmPassword) {
        this.showError('registerAlert', 'registerAlertMessage', 'Passwords do not match.');
        return;
      }

      try {
        await apiClient.post('/api/register', { username, password, role });
        window.location.href = 'login.html';
      } catch (err) {
        this.showError('registerAlert', 'registerAlertMessage', err.message);
      }
    });
  }

  showError(boxId, messageId, message) {
    const box = document.getElementById(boxId);
    const msg = document.getElementById(messageId);
    if (msg) msg.textContent = message;
    if (box) box.classList.remove('hidden');
  }
}

new AuthView();
