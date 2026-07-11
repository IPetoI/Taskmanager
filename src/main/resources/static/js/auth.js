import {showAppMessage, showLoginPage, showAppPage, resetTaskDefaults, resetFilters} from './ui.js';
import {persistAuth, persistRole, clearAuth, state} from './state.js';
import {loadTasks} from './tasks.js';
import {setLanguage} from './app.js';
import {clearAdminState, loadAdminData} from './admin.js';
import {el} from './utils.js';
import {t} from './i18n.js';


const PASSWORD_RULES = [
    {
        key: 'length',
        test: (pw) => pw.length >= 8,
        labelKey: 'auth.ruleLength',
    },
    {
        key: 'uppercase',
        test: (pw) => /[A-Z]/.test(pw),
        labelKey: 'auth.ruleUppercase',
    },
    {
        key: 'lowercase',
        test: (pw) => /[a-z]/.test(pw),
        labelKey: 'auth.ruleLowercase',
    },
    {
        key: 'number',
        test: (pw) => /[0-9]/.test(pw),
        labelKey: 'auth.ruleNumber',
    },
    {
        key: 'special',
        test: (pw) => /[^A-Za-z0-9]/.test(pw),
        labelKey: 'auth.ruleSpecial',
    },
];

/**
 * Returns whether all password rules are satisfied.
 * If so, the password is valid for submission.
 */
function isPasswordValid(password) {
    return PASSWORD_RULES.every((rule) => rule.test(password));
}

export function onPasswordInput() {
    const password = el('registerPassword').value;
    const strengthEl = el('passwordStrength');
    const fillEl = el('strengthFill');
    const rulesEl = el('passwordRules');

    if (!password) {
        strengthEl.classList.add('hidden');
        return;
    }

    strengthEl.classList.remove('hidden');

    const passed = PASSWORD_RULES.filter((rule) => rule.test(password)).length;
    const percent = Math.round((passed / PASSWORD_RULES.length) * 100);

    fillEl.style.width = `${percent}%`;
    fillEl.className = 'strength-fill ' + (
        percent <= 40 ? 'strength-weak' :
            percent <= 79 ? 'strength-medium' :
                'strength-strong'
    );

    rulesEl.innerHTML = PASSWORD_RULES.map((rule) => {
        const ok = rule.test(password);
        return `<li class="rule-item ${ok ? 'rule-ok' : 'rule-fail'}">
            ${ok ? '✓' : '✗'} ${t(rule.labelKey)}
        </li>`;
    }).join('');

    // The confirmation field is also updated if it already contains a value.
    onConfirmPasswordInput();
}

export function onConfirmPasswordInput() {
    const password = el('registerPassword').value;
    const confirm = el('registerConfirmPassword').value;
    const hint = el('confirmPasswordHint');

    if (!confirm) {
        hint.classList.add('hidden');
        return;
    }

    if (password !== confirm) {
        hint.classList.remove('hidden');
    } else {
        hint.classList.add('hidden');
    }
}

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
    const password = el('registerPassword').value;
    const confirmPassword = el('registerConfirmPassword').value;

    if (!username || !email || !password || !confirmPassword) {
        showRegisterError(t('messages.registerRequired'));
        return;
    }

    if (username.length < 3 || username.length > 30) {
        showRegisterError(t('messages.usernameLength'));
        return;
    }

    if (!isPasswordValid(password)) {
        showRegisterError(t('messages.passwordWeak'));
        return;
    }

    if (password !== confirmPassword) {
        showRegisterError(t('auth.passwordMismatch'));
        return;
    }

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, email, password, confirmPassword}),
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            const msg = error.message || '';
            if (msg.includes('Username already exists')) {
                showRegisterError(t('messages.usernameTaken'));
            } else if (msg.includes('Email already exists')) {
                showRegisterError(t('messages.emailTaken'));
            } else {
                showRegisterError(error.message || t('messages.registerFailed'));
            }
            return;
        }

        showRegisterSuccess(t('messages.registerSuccess'));
        el('registerForm').reset();
        el('passwordStrength').classList.add('hidden');
        el('confirmPasswordHint').classList.add('hidden');

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
    el('passwordStrength').classList.add('hidden');
    el('confirmPasswordHint').classList.add('hidden');
    el('loginInfo').className = 'info-text';
    el('registerInfo').className = 'info-text';
    el('loginInfo').textContent = '';
    el('registerInfo').textContent = '';
    showLoginPage();
    showAppMessage('');
}