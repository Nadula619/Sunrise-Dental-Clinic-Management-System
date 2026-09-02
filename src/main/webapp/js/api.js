/**
 * Sunrise Dental Clinic - REST Web Services API Client
 */
const API = {
  baseUrl: window.location.origin + (window.location.pathname.startsWith('/sunrise-dental-sys') ? '/sunrise-dental-sys' : '') + '/api',

  async request(endpoint, options = {}) {
    const defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    };

    options.headers = { ...defaultHeaders, ...(options.headers || {}) };

    try {
      const response = await fetch(`${this.baseUrl}${endpoint}`, options);
      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || (data.details ? data.details.join(', ') : 'Server communication error'));
      }

      return data;
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  },

  // Auth endpoints
  Auth: {
    login: (credentials) => API.request('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
    logout: () => API.request('/auth/logout', { method: 'POST' }),
    getSession: () => API.request('/auth/session', { method: 'GET' }),
    getStaffList: () => API.request('/auth/staff', { method: 'GET' })
  },

  // Appointments endpoints
  Appointments: {
    getAll: () => API.request('/appointments', { method: 'GET' }),
    getById: (id) => API.request(`/appointments/${encodeURIComponent(id)}`, { method: 'GET' }),
    search: (query) => API.request(`/appointments?search=${encodeURIComponent(query)}`, { method: 'GET' }),
    getByDate: (date) => API.request(`/appointments?date=${encodeURIComponent(date)}`, { method: 'GET' }),
    getByDentist: (dentist) => API.request(`/appointments?dentist=${encodeURIComponent(dentist)}`, { method: 'GET' }),
    create: (appointment) => API.request('/appointments', { method: 'POST', body: JSON.stringify(appointment) }),
    update: (id, appointment) => API.request(`/appointments/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(appointment) }),
    updateStatus: (id, status) => API.request(`/appointments/${encodeURIComponent(id)}/status`, { method: 'POST', body: JSON.stringify({ status }) })
  },

  // Billing endpoints
  Billing: {
    getAll: () => API.request('/billing', { method: 'GET' }),
    getByBillNumber: (billNumber) => API.request(`/billing/${encodeURIComponent(billNumber)}`, { method: 'GET' }),
    getByAppointment: (apptNumber) => API.request(`/billing/appointment/${encodeURIComponent(apptNumber)}`, { method: 'GET' }),
    calculate: (billDraft) => API.request('/billing/calculate', { method: 'POST', body: JSON.stringify(billDraft) }),
    create: (bill) => API.request('/billing', { method: 'POST', body: JSON.stringify(bill) }),
    updatePayment: (billNumber, status, method) => API.request(`/billing/${encodeURIComponent(billNumber)}/payment`, {
      method: 'POST',
      body: JSON.stringify({ status, method })
    })
  },

  // Reference Catalogs
  Catalog: {
    getTreatments: () => API.request('/treatments', { method: 'GET' }),
    getDentists: () => API.request('/dentists', { method: 'GET' })
  },

  // Reports endpoint
  Reports: {
    getSummary: () => API.request('/reports', { method: 'GET' })
  }
};
