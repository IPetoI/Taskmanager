import {showAppMessage, showLoginPage, showAppPage, resetTaskDefaults, resetFilters} from './ui.js';
import {persistAuth, persistRole, clearAuth, state} from './state.js';
import {loadTasks} from './tasks.js';
import {setLanguage} from './app.js';
import {clearAdminState, loadAdminData} from './admin.js';
import {el} from './utils.js';
import {t} from './i18n.js';


export function showLoginError(text) {
    const node = el('loginInfo');
    node.textContent = text;
    node.className = 'info-text error';
}

export function showLoginSuccess(text) {
    const node = el('loginInfo');
    node.textContent = text;
    node.className = 'info-text success';
}

export function showRegisterError(text) {
    const node = el('registerInfo');
    node.textContent = text;
    node.className = 'info-text error';
}

export function showRegisterSuccess(text) {
    const node = el('registerInfo');
    node.textContent = text;
    node.className = 'info-text success';
}

export async function registerUser(event) {
    event.preventDefault();
    const username = el('registerUsername').value.trim();
    const email = el('registerEmail').value.trim();
    const password = el('registerPassword').value.trim();

    if (!username || !email || !password) {
        showRegisterError(t('messages.registerRequired'));
        return;
    }

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, email, password}),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            const msg = error.message || '';
            if (msg.includes('Username already exists')) {
                showRegisterError(t('messages.usernameTaken'));
            } else if (msg.includes('Email already exists')) {
                showRegisterError(t('messages.emailTaken'));
            } else if (msg.includes('Invalid request payload')) {
                showRegisterError(t('messages.invalidPayload'));
            } else {
                showRegisterError(error.message ? error.message : t('messages.registerFailed'));
            }
            return;
        }

        showRegisterSuccess(t('messages.registerSuccess'));
        el('registerForm').reset();
        setTimeout(() => {
            el('registerSection').classList.add('hidden');
            el('loginSection').classList.remove('hidden');
            el('loginUsername').value = username;
            el('loginPassword').value = '';
            el('loginPassword').focus();
        }, 1400);
    } catch (err) {
        showRegisterError(`${t('messages.registerFailed')}: ${err.message}`);
    }
}

export async function loginUser(event) {
    event.preventDefault();
    const username = el('loginUsername').value.trim();
    const password = el('loginPassword').value.trim();

    if (!username || !password) {
        showLoginError(t('messages.loginRequired'));
        return;
    }

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, password}),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            const msg = error.message || '';
            if (msg.includes('Invalid username or password')) {
                showLoginError(t('messages.invalidCredentials'));
            } else if (msg.includes('Invalid request payload')) {
                showLoginError(t('messages.invalidPayload'));
            } else {
                showLoginError(error.message ? error.message : t('messages.loginFailed'));
            }
            return;
        }

        const responseData = await response.json();
        const user = responseData.user || responseData;
        const usernameFromBackend = user.username || responseData.username;
        const tokenFromBackend = responseData.token;
        const roleFromBackend = user.role || 'USER';

        if (!usernameFromBackend || !tokenFromBackend) {
            showLoginError(t('messages.loginIncomplete'));
            return;
        }

        persistAuth(tokenFromBackend, usernameFromBackend);
        persistRole(roleFromBackend);
        if (state.role === 'ADMIN') {
            state.adminVisible = true;
        }
        showLoginSuccess(t('messages.loginSuccess'));
        el('loginForm').reset();
        resetTaskDefaults();
        resetFilters();
        showAppMessage('');

        setTimeout(() => {
            showAppPage();
            if (state.role === 'ADMIN') {
                loadAdminData();
            } else {
                loadTasks();
            }
        }, 400);
    } catch (err) {
        showLoginError(`${t('messages.loginFailed')}: ${err.message}`);
    }
}

export function logout() {
    if (!confirm(t('messages.logoutConfirm'))) {
        return;
    }
    clearAuth();
    state.editingTaskId = null;
    state.tasks = [];
    clearAdminState();
    setLanguage('hu', {persist: false, rerender: false});
    resetFilters();
    resetTaskDefaults();
    el('loginForm').reset();
    el('registerForm').reset();
    el('loginInfo').className = 'info-text';
    el('registerInfo').className = 'info-text';
    el('loginInfo').textContent = '';
    el('registerInfo').textContent = '';
    showLoginPage();
    showAppMessage('');
}