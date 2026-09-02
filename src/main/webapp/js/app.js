/**
 * Sunrise Dental Clinic - Global Application Helpers & UI Controller
 */

// Toast Manager
const Toast = {
  show(message, type = 'success', duration = 4000) {
    let container = document.querySelector('.toast-container');
    if (!container) {
      container = document.createElement('div');
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = '✓';
    if (type === 'error') icon = '✕';
    if (type === 'warning') icon = '⚠';

    toast.innerHTML = `
      <div style="font-weight:bold; font-size:16px;">${icon}</div>
      <div style="flex:1;">${message}</div>
    `;

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }
};

// Modal Manager
const Modal = {
  open(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.add('active');
    }
  },
  close(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
      modal.classList.remove('active');
    }
  },
  initClosers() {
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
          overlay.classList.remove('active');
        }
      });
    });
    document.querySelectorAll('.modal-close, [data-dismiss="modal"]').forEach(btn => {
      btn.addEventListener('click', () => {
        const overlay = btn.closest('.modal-overlay');
        if (overlay) overlay.classList.remove('active');
      });
    });
  }
};

// Session & Auth Controller
async function checkAuth(requiresAuth = true) {
  try {
    const res = await API.Auth.getSession();
    if (res.success && res.data) {
      const user = res.data;
      sessionStorage.setItem('user', JSON.stringify(user));
      updateUserUI(user);
      applyRolePermissions(user);
      return user;
    }
  } catch (err) {
    if (requiresAuth && !window.location.pathname.endsWith('login.html')) {
      window.location.href = 'login.html';
    }
  }
  return null;
}

function getCurrentUser() {
  const stored = sessionStorage.getItem('user');
  if (stored) {
    try { return JSON.parse(stored); } catch (e) {}
  }
  return null;
}

function updateUserUI(user) {
  const nameEl = document.getElementById('userNameDisplay');
  const roleEl = document.getElementById('userRoleDisplay');
  const avatarEl = document.getElementById('userAvatarDisplay');

  if (nameEl) nameEl.textContent = user.fullName || user.username;
  if (roleEl) roleEl.textContent = user.role;
  if (avatarEl) {
    const initials = (user.fullName || user.username)
      .split(' ')
      .map(n => n[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
    avatarEl.textContent = initials || 'U';
  }
}

/**
 * Strict Role-Based Access Control (RBAC) Enforcement
 */
function applyRolePermissions(user) {
  if (!user || !user.role) return;

  const role = user.role.toUpperCase();
  const currentPath = window.location.pathname;

  // Sidebar Links
  const billingLink = document.querySelector('a[href="billing.html"]');
  const reportsLink = document.querySelector('a[href="reports.html"]');
  const staffLink = document.querySelector('a[href="staff.html"]');

  if (role === 'DENTIST') {
    // Dentists are medical practitioners: No billing, No reports, No staff admin
    if (billingLink) billingLink.style.display = 'none';
    if (reportsLink) reportsLink.style.display = 'none';
    if (staffLink) staffLink.style.display = 'none';

    // Restrict direct URL access
    if (currentPath.endsWith('billing.html') || currentPath.endsWith('reports.html') || currentPath.endsWith('staff.html')) {
      alert('Access Restricted: This section is restricted to Administration and Reception staff.');
      window.location.href = 'appointments.html';
      return;
    }

    // Hide "Register New Appointment" button for Dentists
    const btnNewAppt = document.getElementById('btnOpenNewAppt');
    if (btnNewAppt) btnNewAppt.style.display = 'none';

  } else if (role === 'RECEPTIONIST') {
    // Receptionists handle front-desk bookings & billing, but cannot view confidential reports or manage staff
    if (reportsLink) reportsLink.style.display = 'none';
    if (staffLink) staffLink.style.display = 'none';

    if (currentPath.endsWith('reports.html') || currentPath.endsWith('staff.html')) {
      alert('Access Restricted: Clinic Management & Staff Administration is restricted to Administrators.');
      window.location.href = 'index.html';
      return;
    }
  } else if (role === 'ADMIN') {
    // Admins see all links
    if (billingLink) billingLink.style.display = 'flex';
    if (reportsLink) reportsLink.style.display = 'flex';
    if (staffLink) staffLink.style.display = 'flex';
  }
}

async function handleLogout() {
  try {
    await API.Auth.logout();
    sessionStorage.removeItem('user');
    Toast.show('Logged out successfully', 'success');
    setTimeout(() => {
      window.location.href = 'login.html';
    }, 500);
  } catch (err) {
    window.location.href = 'login.html';
  }
}

// Formatters
const Format = {
  currency(amount) {
    return 'LKR ' + (Number(amount) || 0).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  },
  date(dateStr) {
    if (!dateStr) return '-';
    return dateStr;
  },
  statusBadge(status) {
    const s = (status || 'SCHEDULED').toUpperCase();
    let badgeClass = 'badge-scheduled';
    if (s === 'IN_PROGRESS') badgeClass = 'badge-in_progress';
    if (s === 'COMPLETED') badgeClass = 'badge-completed';
    if (s === 'CANCELLED') badgeClass = 'badge-cancelled';
    if (s === 'PAID') badgeClass = 'badge-paid';
    if (s === 'PENDING') badgeClass = 'badge-pending';

    return `<span class="badge ${badgeClass}">${s.replace('_', ' ')}</span>`;
  }
};

document.addEventListener('DOMContentLoaded', () => {
  Modal.initClosers();
  const logoutBtn = document.getElementById('logoutBtn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', handleLogout);
  }
});
