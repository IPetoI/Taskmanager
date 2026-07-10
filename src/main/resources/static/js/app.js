import {applyTranslations, showLoginPage, showAppPage, resetTaskDefaults, resetFilters, toggleAuthForm} from './ui.js';
import {renderCalendar, setCalendarView, navigateCalendar, goToToday} from './calendar.js';
import {state, clearAuth, clearLanguage, persistLanguage} from './state.js';
import {renderTasksTable, loadTasks, saveTask, initTaskEvents} from './tasks.js';
import {loginUser, registerUser, logout} from './auth.js';
import {renderAdminTable, loadAdminData, setAdminView, setAdminSearch, submitEditForm, closeEditCard, initAdminEvents, changeAdminPage} from './admin.js';
import {el, toDatetimeLocal} from './utils.js';


/**
 * Sets the active language, optionally saves it to localStorage, and rerenders
 * UI elements that require translation (table, calendar). During initialization,
 * it is called with `rerender: false` because there is no loaded data to redraw yet.
 */
export function setLanguage(lang, {persist = true, rerender = true} = {}) {
    state.language = lang === 'en' ? 'en' : 'hu';
    if (persist) {
        persistLanguage(state.language);
    }
    applyTranslations();
    if (rerender) {
        renderAdminTable();
        if (state.role !== 'ADMIN') {
            renderTasksTable(state.tasks);
            renderCalendar();
        }
    }
}

/**
 * If the user provides a start time and has not manually edited the due date yet,
 * automatically sets the due date to one hour after the start date.
 * If the user has already set a due date manually, it will not be overwritten.
 */
function onStartDateChange() {
    const startRaw = el('taskStartDate').value;
    if (!startRaw) return;

    const endInput = el('taskEndDate');
    if (state.endDateUserEdited && endInput.value) return;

    const start = new Date(startRaw);
    if (Number.isNaN(start.getTime())) return;

    const end = new Date(start.getTime() + 60 * 60 * 1000);
    endInput.value = toDatetimeLocal(end);
}

function bindEvents() {
    el('loginForm').addEventListener('submit', loginUser);
    el('showRegisterLink').addEventListener('click', toggleAuthForm);
    el('registerForm').addEventListener('submit', registerUser);
    el('showLoginLink').addEventListener('click', toggleAuthForm);
    el('logoutBtn').addEventListener('click', logout);
    el('loginLangBtn')
        .addEventListener('click', () => setLanguage(state.language === 'hu' ? 'en' : 'hu'));
    el('appLangBtn')
        .addEventListener('click', () => setLanguage(state.language === 'hu' ? 'en' : 'hu'));
    el('adminUsersBtn').addEventListener('click', () => setAdminView('users'));
    el('adminTasksBtn').addEventListener('click', () => setAdminView('tasks'));
    el('adminRefreshBtn').addEventListener('click', loadAdminData);
    el('adminSearch').addEventListener('input', (event) => setAdminSearch(event.target.value));
    el('adminEditForm').addEventListener('submit', submitEditForm);
    el('adminEditCancelBtn').addEventListener('click', closeEditCard);
    el('taskForm').addEventListener('submit', saveTask);
    el('refreshBtn').addEventListener('click', loadTasks);
    el('cancelEditBtn').addEventListener('click', () => {
        state.editingTaskId = null;
        el('taskForm').reset();
        resetTaskDefaults();
    });
    el('taskStartDate').addEventListener('change', onStartDateChange);
    // Ha a user manuálisan szerkeszti a határidőt, ezt megjegyezzük,
    // hogy a startDate változásakor ne írjuk felül.
    el('taskEndDate').addEventListener('change', () => { state.endDateUserEdited = true; });
    el('calendarWeekBtn').addEventListener('click', () => setCalendarView('week'));
    el('calendarMonthBtn').addEventListener('click', () => setCalendarView('month'));
    el('calendarPrevBtn').addEventListener('click', () => navigateCalendar(-1));
    el('calendarNextBtn').addEventListener('click', () => navigateCalendar(1));
    el('calendarTodayBtn').addEventListener('click', goToToday);
    initTaskEvents();
    initAdminEvents();
}

function init() {
    bindEvents();
    setLanguage(state.language, {persist: false, rerender: false});
    resetTaskDefaults();
    resetFilters();
    applyTranslations();

    if (state.username && state.token) {
        if (state.role === 'ADMIN') {
            state.adminVisible = true;
        }
        showAppPage();
        if (state.role === 'ADMIN') {
            loadAdminData();
        } else {
            loadTasks();
        }
    } else {
        clearAuth();
        clearLanguage();
        setLanguage('hu', {persist: false, rerender: false});
        showLoginPage();
    }
}

document.addEventListener('DOMContentLoaded', init);