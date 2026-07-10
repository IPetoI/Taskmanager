import {el, elExists, defaultTaskStart, defaultTaskEnd, toDatetimeLocal} from './utils.js';
import {state, clearAuth} from './state.js';
import {t} from './i18n.js';


// Keep auto-hide timers for the message bar at module scope,
// so consecutive showAppMessage() calls do not interfere with each other.
let messageHideTimer = null;
let messageClearTimer = null;


export function updateLanguageButtons() {
    const label = state.language === 'hu' ? 'EN' : 'HU';
    if (elExists('loginLangBtn')) {
        el('loginLangBtn').textContent = label;
    }
    if (elExists('appLangBtn')) {
        el('appLangBtn').textContent = label;
    }
}

export function updateCurrentUserBadge() {
    const badge = el('currentUserBadge');
    if (!badge) {
        return;
    }
    badge.textContent = state.username
        ? t('auth.loggedInAs', {username: state.username})
        : t('auth.notLoggedIn');
}

export function updateTaskFormMode() {
    const heading = document.querySelector('[data-i18n="task.newHeading"]');
    if (heading) {
        heading.textContent = state.editingTaskId
            ? t('task.editHeading')
            : t('task.newHeading');
    }
    if (elExists('taskSubmitBtn')) {
        el('taskSubmitBtn').textContent = state.editingTaskId
            ? t('task.saveBtn')
            : t('task.createBtn');
    }
    if (elExists('cancelEditBtn')) {
        el('cancelEditBtn').classList.toggle('hidden', !state.editingTaskId);
    }
}

export function updateRoleBasedVisibility() {
    const adminMode = state.role === 'ADMIN';
    const userControls = el('userControlsSection');
    const userTasks = el('userTasksCard');
    const tasksList = elExists('tasksListCard') ? el('tasksListCard') : null;
    const calendar = el('calendarCard');
    const adminCard = el('adminCard');

    [userControls, userTasks, tasksList, calendar].forEach((node) => {
        if (node) {
            node.classList.toggle('hidden', adminMode);
        }
    });
    if (adminCard) {
        adminCard.classList.toggle('hidden', !adminMode);
    }
}

export function applyTranslations() {
    document.documentElement.lang = state.language;
    document.title = t('page.title');

    document.querySelectorAll('[data-i18n]').forEach((node) => {
        const key = node.dataset.i18n;
        if (node.id === 'currentUserBadge') {
            return;
        }
        node.textContent = t(key);
    });

    document.querySelectorAll('[data-i18n-placeholder]').forEach((node) => {
        node.placeholder = t(node.dataset.i18nPlaceholder);
    });

    updateLanguageButtons();
    updateCurrentUserBadge();
    updateTaskFormMode();
    updateRoleBasedVisibility();
}

export function handleUnauthorized() {
    clearAuth();
    state.tasks = [];
    showLoginPage();
    showAppMessage(t('messages.sessionExpired'), true);
}

export function showAppMessage(text, isError = false) {
    const msg = el('appMessage');

    if (messageHideTimer) {
        clearTimeout(messageHideTimer);
        messageHideTimer = null;
    }
    if (messageClearTimer) {
        clearTimeout(messageClearTimer);
        messageClearTimer = null;
    }
    if (!text) {
        clearAppMessage(msg);
        return;
    }

    msg.textContent = text;
    msg.className = `message-container show ${isError ? 'error' : 'success'}`;
    messageHideTimer = setTimeout(() => {
        msg.classList.remove('show');
        messageClearTimer = setTimeout(() => {
            msg.className = 'message-container';
            msg.textContent = '';
            messageClearTimer = null;
        }, 1000);
        messageHideTimer = null;
    }, 5000);
}

export function clearAppMessage(msg) {
    msg.classList.remove('show');
    messageClearTimer = setTimeout(() => {
        msg.className = 'message-container';
        msg.textContent = '';
        messageClearTimer = null;
    }, 300);
}

export function resetTaskDefaults() {
    if (elExists('taskPriority')) {
        el('taskPriority').value = 'MEDIUM';
    }
    if (elExists('taskStatus')) {
        el('taskStatus').value = 'TODO';
    }
    if (elExists('taskStartDate')) {
        el('taskStartDate').value = toDatetimeLocal(defaultTaskStart());
    }
    if (elExists('taskEndDate')) {
        el('taskEndDate').value = toDatetimeLocal(defaultTaskEnd());
    }
    // Recurrence defaults
    if (elExists('taskRecurrenceUnit')) {
        el('taskRecurrenceUnit').value = 'NONE';
    }
    state.editingTaskId = null;
    state.endDateUserEdited = false;
    updateTaskFormMode();
}

export function resetFilters() {
    if (elExists('filterStatus')) {
        el('filterStatus').value = '';
    }
    if (elExists('filterPriority')) {
        el('filterPriority').value = '';
    }
    if (elExists('filterDateFrom')) {
        el('filterDateFrom').value = '';
    }
    if (elExists('filterDateTo')) {
        el('filterDateTo').value = '';
    }
}

export function showLoginPage() {
    el('loginPage').classList.remove('hidden');
    el('appPage').classList.add('hidden');
}

export function showAppPage() {
    el('loginPage').classList.add('hidden');
    el('appPage').classList.remove('hidden');
    updateCurrentUserBadge();
    updateRoleBasedVisibility();
}

export function toggleAuthForm(event) {
    event.preventDefault();
    el('loginSection').classList.toggle('hidden');
    el('registerSection').classList.toggle('hidden');

    el('loginInfo').className = 'info-text';
    el('registerInfo').className = 'info-text';
    el('loginInfo').textContent = '';
    el('registerInfo').textContent = '';
}