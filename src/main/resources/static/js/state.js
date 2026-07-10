// Manages the global client-side state and localStorage-based persistence.
// The state object serves as the application's single source of truth.

const STORAGE_KEYS = {
    token: 'taskmanager.token',
    username: 'taskmanager.username',
    language: 'taskmanager.language',
};

export const state = {
    token: normalizeStoredValue(localStorage.getItem(STORAGE_KEYS.token)),
    username: normalizeStoredValue(localStorage.getItem(STORAGE_KEYS.username)),
    role: normalizeStoredValue(localStorage.getItem('taskmanager.role')) || 'USER',
    language: normalizeStoredValue(localStorage.getItem(STORAGE_KEYS.language)) || 'hu',
    editingTaskId: null,
    endDateUserEdited: false,
    tasks: [],
    tasksPerPage: 10,
    tasksPage: 1,
    tasksSort: {column: 'startDate', dir: 'asc'},
    calendarView: 'week',
    calendarCursor: startOfCurrentWeek(),
    adminVisible: false,
    adminView: 'users',
    adminSearch: '',
    adminUsers: [],
    adminTasks: [],
    adminLoaded: false,
    adminPage: 1,
    adminPerPage: 10,
    adminSort: {
        users: {column: 'username', dir: 'asc'},
        tasks: {column: 'title', dir: 'asc'},
    },
};

/**
 * localStorage returns all values as strings, and in case of missing keys it may return
 * the string literals `"null"` or `"undefined"` due to previous incorrect saves.
 * This function normalizes these values to an empty string so that state fields
 * consistently evaluate to a "falsy" value when no data is available.
 */
export function normalizeStoredValue(value) {
    if (value === null || value === undefined || value === 'null' || value === 'undefined') {
        return '';
    }
    return value;
}

// Start of the current week (Monday at 00:00) - used as the calendar initial view.
function startOfCurrentWeek() {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    const day = d.getDay();
    const mondayIndex = day === 0 ? 6 : day - 1;
    d.setDate(d.getDate() - mondayIndex);
    return d;
}

export function persistAuth(token, username) {
    state.token = normalizeStoredValue(token);
    state.username = normalizeStoredValue(username);

    if (state.token) {
        localStorage.setItem(STORAGE_KEYS.token, state.token);
    } else {
        localStorage.removeItem(STORAGE_KEYS.token);
    }
    if (state.username) {
        localStorage.setItem(STORAGE_KEYS.username, state.username);
    } else {
        localStorage.removeItem(STORAGE_KEYS.username);
    }
}

export function persistRole(role) {
    state.role = role === 'ADMIN' ? 'ADMIN' : 'USER';
    localStorage.setItem('taskmanager.role', state.role);
}

export function clearAuth() {
    persistAuth('', '');
    state.role = 'USER';
    localStorage.removeItem('taskmanager.role');
}

export function persistLanguage(lang) {
    state.language = lang === 'en' ? 'en' : 'hu';
    localStorage.setItem(STORAGE_KEYS.language, state.language);
}

export function clearLanguage() {
    localStorage.removeItem(STORAGE_KEYS.language);
}

/**
 * Adds the current JWT as an Authorization header to the request.
 * The backend identifies the authenticated user from this token - the frontend
 * never sends a separate "username" field or parameter for protected task endpoints.
 */
export function authHeaders(extraHeaders = {}) {
    const headers = {...extraHeaders};
    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }
    return headers;
}