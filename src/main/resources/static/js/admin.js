import {el, elExists, escapeHtml, formatDateTime} from './utils.js';
import {handleUnauthorized, showAppMessage} from './ui.js';
import {state, authHeaders} from './state.js';
import {sortByColumn} from './sorting.js';
import {bindActions} from './dom-events.js';
import {paginateItems} from './tasks.js';
import {populateTaskFields, readTaskFieldsPayload} from './task-fields.js';
import {t} from './i18n.js';

function normalize(value) {
    return String(value === null || value === undefined ? '' : value).toLowerCase();
}

const ADMIN_SORT_TYPES = {
    id: 'number',
    startDate: 'date',
    endDate: 'date',
};

function sortRows(rows, mode) {
    const sortConfig = state.adminSort[mode] || {column: 'id', dir: 'asc'};
    const locale = state.language === 'en' ? 'en' : 'hu';
    return sortByColumn(rows, sortConfig.column, sortConfig.dir, ADMIN_SORT_TYPES, locale);
}

function filterUsers(users) {
    const term = normalize(state.adminSearch);
    if (!term) {
        return users;
    }

    return users.filter((user) => {
        const haystack = [
            user.id,
            user.username,
            user.email,
        ].map(normalize).join(' ');
        return haystack.includes(term);
    });
}

function filterTasks(tasks) {
    const term = normalize(state.adminSearch);
    if (!term) {
        return tasks;
    }

    return tasks.filter((task) => {
        const haystack = [
            task.id,
            task.title,
            task.description,
            task.username,
            task.status,
            task.priority,
            task.startDate ? formatDateTime(task.startDate) : '',
            task.endDate ? formatDateTime(task.endDate) : '',
            task.startDate,
            task.endDate,
        ].map(normalize).join(' ');
        return haystack.includes(term);
    });
}

function updateModeButtons() {
    const isUsers = state.adminView === 'users';
    const usersBtn = elExists('adminUsersBtn') ? el('adminUsersBtn') : null;
    const tasksBtn = elExists('adminTasksBtn') ? el('adminTasksBtn') : null;

    if (usersBtn) {
        usersBtn.classList.toggle('active', isUsers);
        usersBtn.setAttribute('aria-pressed', isUsers ? 'true' : 'false');
    }
    if (tasksBtn) {
        tasksBtn.classList.toggle('active', !isUsers);
        tasksBtn.setAttribute('aria-pressed', !isUsers ? 'true' : 'false');
    }
}

function renderUsersTable(users) {
    const head = el('adminTableHead');
    const body = el('adminTableBody');
    const sortConfig = state.adminSort.users;
    const headers = [
        {key: 'id', label: t('admin.userId'), type: 'number'},
        {key: 'username', label: t('admin.userUsername')},
        {key: 'email', label: t('admin.userEmail')},
    ];

    head.innerHTML = `
        <tr>
            ${headers.map((header) => {
        const active = sortConfig.column === header.key;
        const indicator = active ? (sortConfig.dir === 'asc' ? '▲' : '▼') : '';
        return `<th class="sortable" data-action="sort-admin" data-id="${header.key}">${escapeHtml(header.label)}<span class="sort-indicator">${indicator}</span></th>`;
    }).join('')}
            <th>${escapeHtml(t('admin.actions'))}</th>
        </tr>
    `;

    if (!users.length) {
        body.innerHTML = `<tr><td colspan="4" class="empty-state">${escapeHtml(t('admin.usersEmpty'))}</td></tr>`;
        renderAdminPagination(0);
        return;
    }

    const {pageItems, totalPages} = paginateItems(users, state.adminPage, state.adminPerPage);
    state.adminPage = Math.min(state.adminPage, totalPages);

    body.innerHTML = pageItems.map((user) => `
        <tr data-row-type="user" data-row-id="${user.id}">
            <td>${escapeHtml(user.id)}</td>
            <td>${escapeHtml(user.username)}</td>
            <td>${escapeHtml(user.email)}</td>
            <td class="admin-row-actions">
                <button type="button" class="btn-secondary" data-action="edit-user" data-id="${user.id}">${escapeHtml(t('action.edit'))}</button>
                <button type="button" class="btn-danger" data-action="delete-user" data-id="${user.id}">${escapeHtml(t('action.delete'))}</button>
            </td>
        </tr>
    `).join('');

    renderAdminPagination(totalPages);
}

function renderTasksTable(tasks) {
    const head = el('adminTableHead');
    const body = el('adminTableBody');
    const sortConfig = state.adminSort.tasks;
    const headers = [
        {key: 'id', label: t('admin.taskId')},
        {key: 'title', label: t('admin.taskTitle')},
        {key: 'description', label: t('admin.taskDescription')},
        {key: 'username', label: t('admin.taskOwner')},
        {key: 'status', label: t('admin.taskStatus')},
        {key: 'priority', label: t('admin.taskPriority')},
        {key: 'startDate', label: t('admin.taskDates')},
    ];

    head.innerHTML = `
        <tr>
            ${headers.map((header) => {
        const active = sortConfig.column === header.key;
        const indicator = active ? (sortConfig.dir === 'asc' ? '▲' : '▼') : '';
        return `<th class="sortable" data-action="sort-admin" data-id="${header.key}">${escapeHtml(header.label)}<span class="sort-indicator">${indicator}</span></th>`;
    }).join('')}
            <th>${escapeHtml(t('admin.actions'))}</th>
        </tr>
    `;

    if (!tasks.length) {
        body.innerHTML = `<tr><td colspan="8" class="empty-state">${escapeHtml(t('admin.tasksEmpty'))}</td></tr>`;
        renderAdminPagination(0);
        return;
    }

    const {pageItems, totalPages} = paginateItems(tasks, state.adminPage, state.adminPerPage);
    state.adminPage = Math.min(state.adminPage, totalPages);

    body.innerHTML = pageItems.map((task) => `
        <tr data-row-type="task" data-row-id="${task.id}">
            <td>${escapeHtml(task.id)}</td>
            <td>${escapeHtml(task.title)}</td>
            <td>${escapeHtml(task.description || '-')}</td>
            <td>${escapeHtml(task.username || '-')}</td>
            <td>${escapeHtml(task.status || '-')}</td>
            <td>${escapeHtml(task.priority || '-')}</td>
            <td>${escapeHtml(task.startDate ? formatDateTime(task.startDate) : '-')}</td>
            <td class="admin-row-actions">
                <button type="button" class="btn-secondary" data-action="edit-task" data-id="${task.id}">${escapeHtml(t('action.edit'))}</button>
                <button type="button" class="btn-danger" data-action="delete-task" data-id="${task.id}">${escapeHtml(t('action.delete'))}</button>
            </td>
        </tr>
    `).join('');

    renderAdminPagination(totalPages);
}

function renderAdminPagination(totalPages) {
    const container = el('adminPagination');
    if (!container) return;
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    const current = state.adminPage;
    const prevDisabled = current <= 1 ? 'disabled' : '';
    const nextDisabled = current >= totalPages ? 'disabled' : '';

    const pages = [];
    const maxButtons = 7;
    if (totalPages <= maxButtons) {
        for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
        const left = Math.max(1, current - 2);
        const right = Math.min(totalPages, current + 2);
        if (left > 1) pages.push(1);
        if (left > 2) pages.push('left-ellipsis');
        for (let i = left; i <= right; i++) pages.push(i);
        if (right < totalPages - 1) pages.push('right-ellipsis');
        if (right < totalPages) pages.push(totalPages);
    }

    const pageButtons = pages.map((p) => {
        if (p === 'left-ellipsis' || p === 'right-ellipsis') {
            return `<span class="pagination-ellipsis">&hellip;</span>`;
        }
        const active = p === current ? 'pagination-page active' : 'pagination-page';
        return `<button class="btn-secondary ${active}" data-action="change-admin-page" data-id="${p}">${p}</button>`;
    }).join('');

    container.innerHTML = `
        <button class="btn-secondary" ${prevDisabled} data-action="change-admin-page" data-id="${current - 1}">&laquo;</button>
        ${pageButtons}
        <button class="btn-secondary" ${nextDisabled} data-action="change-admin-page" data-id="${current + 1}">&raquo;</button>
    `;
}

export function renderAdminTable() {
    if (!elExists('adminCard')) {
        return;
    }

    updateModeButtons();

    const searchInput = elExists('adminSearch') ? el('adminSearch') : null;
    if (searchInput && searchInput.value !== state.adminSearch) {
        searchInput.value = state.adminSearch;
    }

    // The visibility of #adminCard is controlled by ui.js (updateRoleBasedVisibility) based
    // on the user's role. This function only determines whether it makes sense to fetch
    // and render the table rows.
    if (!state.adminVisible) {
        return;
    }

    if (state.adminView === 'users') {
        const filtered = sortRows(filterUsers(state.adminUsers), 'users');
        renderUsersTable(filtered);
    } else {
        const filtered = sortRows(filterTasks(state.adminTasks), 'tasks');
        renderTasksTable(filtered);
    }
}

export async function loadAdminData() {
    if (!state.token) {
        handleUnauthorized();
        return;
    }

    try {
        const [usersResponse, tasksResponse] = await Promise.all([
            fetch('/api/admin/users', {headers: authHeaders()}),
            fetch('/api/admin/tasks', {headers: authHeaders()}),
        ]);

        if (usersResponse.status === 401 || usersResponse.status === 403
            || tasksResponse.status === 401 || tasksResponse.status === 403) {
            handleUnauthorized();
            return;
        }

        if (!usersResponse.ok || !tasksResponse.ok) {
            showAppMessage(t('admin.loadFailed'), true);
            return;
        }

        state.adminUsers = await usersResponse.json();
        state.adminTasks = await tasksResponse.json();
        state.adminLoaded = true;
        renderAdminTable();
    } catch (err) {
        showAppMessage(`${t('admin.loadFailed')}: ${err.message}`, true);
    }
}

export function setAdminView(view) {
    state.adminView = view === 'tasks' ? 'tasks' : 'users';
    state.adminPage = 1;
    renderAdminTable();
}

export function setAdminSearch(value) {
    state.adminSearch = String(value || '');
    state.adminPage = 1;
    renderAdminTable();
}

export function changeAdminPage(page) {
    state.adminPage = Math.max(1, page);
    renderAdminTable();
}

export function sortAdminBy(column) {
    const mode = state.adminView === 'tasks' ? 'tasks' : 'users';
    const current = state.adminSort[mode] || {column, dir: 'asc'};

    if (current.column === column) {
        current.dir = current.dir === 'asc' ? 'desc' : 'asc';
    } else {
        current.column = column;
        current.dir = 'asc';
    }

    state.adminSort[mode] = current;
    renderAdminTable();
}

export function clearAdminState() {
    state.adminVisible = false;
    state.adminView = 'users';
    state.adminSearch = '';
    state.adminUsers = [];
    state.adminTasks = [];
    state.adminLoaded = false;
    state.adminPage = 1;
    state.adminSort = {
        users: {column: 'username', dir: 'asc'},
        tasks: {column: 'title', dir: 'asc'},
    };
    closeEditCard();
    renderAdminTable();
}

function findUserById(id) {
    return state.adminUsers.find((u) => String(u.id) === String(id));
}

function findTaskById(id) {
    return state.adminTasks.find((task) => String(task.id) === String(id));
}

export function openEditUser(id) {
    const user = findUserById(id);
    if (!user) {
        return;
    }

    el('adminEditId').value = user.id;
    el('adminEditType').value = 'user';
    el('adminEditHeading').textContent = t('admin.editUserHeading');
    el('adminEditUsername').value = user.username || '';
    el('adminEditEmail').value = user.email || '';
    el('adminEditPassword').value = '';

    el('adminEditUserFields').classList.remove('hidden');
    el('adminEditTaskFields').classList.add('hidden');
    el('adminEditCard').classList.remove('hidden');
    el('adminEditCard').scrollIntoView({behavior: 'smooth', block: 'start'});
}

export function openEditTask(id) {
    const task = findTaskById(id);
    if (!task) {
        return;
    }

    el('adminEditId').value = task.id;
    el('adminEditType').value = 'task';
    el('adminEditHeading').textContent = t('admin.editTaskHeading');
    populateTaskFields('adminEditTask', task);

    el('adminEditUserFields').classList.add('hidden');
    el('adminEditTaskFields').classList.remove('hidden');
    el('adminEditCard').classList.remove('hidden');
    el('adminEditCard').scrollIntoView({behavior: 'smooth', block: 'start'});
}

export function closeEditCard() {
    if (!elExists('adminEditCard')) {
        return;
    }
    el('adminEditCard').classList.add('hidden');
    el('adminEditForm').reset();
}

export async function submitEditForm(event) {
    event.preventDefault();

    if (!state.token) {
        handleUnauthorized();
        return;
    }

    const type = el('adminEditType').value;
    const id = el('adminEditId').value;

    try {
        let response;
        if (type === 'user') {
            const payload = {
                username: el('adminEditUsername').value.trim(),
                email: el('adminEditEmail').value.trim(),
                password: el('adminEditPassword').value,
            };
            response = await fetch(`/api/admin/users/${id}`, {
                method: 'PUT',
                headers: authHeaders({'Content-Type': 'application/json'}),
                body: JSON.stringify(payload),
            });
        } else {
            const payload = readTaskFieldsPayload('adminEditTask');
            response = await fetch(`/api/admin/tasks/${id}`, {
                method: 'PUT',
                headers: authHeaders({'Content-Type': 'application/json'}),
                body: JSON.stringify(payload),
            });
        }

        if (response.status === 401 || response.status === 403) {
            handleUnauthorized();
            return;
        }
        if (!response.ok) {
            showAppMessage(t('admin.updateFailed'), true);
            return;
        }

        showAppMessage(t('admin.updateSuccess'), false);
        closeEditCard();
        await loadAdminData();
        scrollToAdminRow(type, id);
    } catch (err) {
        showAppMessage(`${t('admin.updateFailed')}: ${err.message}`, true);
    }
}

/**
 * After saving, scrolls back to the edited row in the table.
 * The `data-row-id` attribute is added to the `<tr>` element by
 * renderUsersTable/renderTasksTable.
 */
function scrollToAdminRow(type, id) {
    const selector = `[data-row-type="${type}"][data-row-id="${id}"]`;
    const row = el('adminTable').querySelector(selector);
    if (row) {
        row.scrollIntoView({behavior: 'smooth', block: 'center'});
        row.classList.add('admin-row-highlight');
        setTimeout(() => row.classList.remove('admin-row-highlight'), 1500);
    }
}

export async function deleteUser(id) {
    if (!window.confirm(t('admin.userDeleteConfirm'))) {
        return;
    }
    if (!state.token) {
        handleUnauthorized();
        return;
    }

    try {
        const response = await fetch(`/api/admin/users/${id}`, {
            method: 'DELETE',
            headers: authHeaders(),
        });
        if (response.status === 401 || response.status === 403) {
            handleUnauthorized();
            return;
        }
        if (!response.ok && response.status !== 204) {
            showAppMessage(t('admin.deleteFailed'), true);
            return;
        }

        showAppMessage(t('admin.deleteSuccess'), false);
        closeEditCard();
        await loadAdminData();
    } catch (err) {
        showAppMessage(`${t('admin.deleteFailed')}: ${err.message}`, true);
    }
}

export async function deleteTask(id) {
    if (!window.confirm(t('task.deleteConfirm'))) {
        return;
    }
    if (!state.token) {
        handleUnauthorized();
        return;
    }

    try {
        const response = await fetch(`/api/admin/tasks/${id}`, {
            method: 'DELETE',
            headers: authHeaders(),
        });
        if (response.status === 401 || response.status === 403) {
            handleUnauthorized();
            return;
        }
        if (!response.ok && response.status !== 204) {
            showAppMessage(t('admin.deleteFailed'), true);
            return;
        }

        showAppMessage(t('admin.deleteSuccess'), false);
        closeEditCard();
        await loadAdminData();
    } catch (err) {
        showAppMessage(`${t('admin.deleteFailed')}: ${err.message}`, true);
    }
}

/**
 * One-time event binding for the admin table (using delegation, see dom-events.js).
 * Called once during app initialization. The table header and rows are rerendered
 * when switching views, but the listener remains intact because it is attached
 * to the stable #adminTable container.
 */
export function initAdminEvents() {
    bindActions(el('adminTable'), {
        'sort-admin': (column) => sortAdminBy(column),
        'edit-user': (id) => openEditUser(Number(id)),
        'delete-user': (id) => deleteUser(Number(id)),
        'edit-task': (id) => openEditTask(Number(id)),
        'delete-task': (id) => deleteTask(Number(id)),
    });
    if (elExists('adminPagination')) {
        bindActions(el('adminPagination'), {
            'change-admin-page': (id) => changeAdminPage(Number(id)),
        });
    }
}