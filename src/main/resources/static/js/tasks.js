import {el, elExists, formatTaskRange, escapeHtml} from './utils.js';
import {handleUnauthorized, showAppMessage, resetTaskDefaults, updateTaskFormMode} from './ui.js';
import {state, authHeaders} from './state.js';
import {renderCalendar} from './calendar.js';
import {compareValues} from './sorting.js';
import {bindActions} from './dom-events.js';
import {populateTaskFields, readTaskFieldsPayload} from './task-fields.js';
import {t} from './i18n.js';


export async function loadTasks() {
    if (!state.token) {
        handleUnauthorized();
        return;
    }

    const status = el('filterStatus').value.trim();
    const priority = el('filterPriority').value.trim();
    const dateFrom = elExists('filterDateFrom') ? el('filterDateFrom').value.trim() : '';
    const dateTo = elExists('filterDateTo') ? el('filterDateTo').value.trim() : '';

// The backend identifies the user from the JWT; the request only sends optional filters.
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (priority) params.set('priority', priority);
    if (dateFrom) params.set('dateFrom', dateFrom);
    if (dateTo) params.set('dateTo', dateTo);
    const query = params.toString();
    const url = query ? `/api/tasks?${query}` : '/api/tasks';

    try {
        const response = await fetch(url, {headers: authHeaders()});
        if (response.status === 401 || response.status === 403) {
            handleUnauthorized();
            return;
        }
        if (!response.ok) {
            showAppMessage(t('messages.loadFailed'), true);
            return;
        }

        const tasks = await response.json();
        state.tasks = Array.isArray(tasks) ? tasks : [];
        renderTasksTable(state.tasks);
        renderCalendar();
        showAppMessage('');
    } catch (err) {
        showAppMessage(`${t('messages.loadFailed')}: ${err.message}`, true);
    }
}

export function getPriorityLabel(priority) {
    const labels = {
        LOW: t('priority.low'),
        MEDIUM: t('priority.medium'),
        HIGH: t('priority.high')
    };
    return labels[priority] || priority;
}

export function getStatusLabel(status) {
    const labels = {
        TODO: t('status.todo'),
        IN_PROGRESS: t('status.inProgress'),
        DONE: t('status.done')
    };
    return labels[status] || status;
}

const TASK_HEADERS = [
    {key: 'title',       i18n: 'table.title',       label: 'Cím'},
    {key: 'description', i18n: 'table.description',  label: 'Leírás'},
    {key: 'startDate',   i18n: 'table.dates',        label: 'Időpont'},
    {key: 'priority',    i18n: 'table.priority',     label: 'Prioritás'},
    {key: 'status',      i18n: 'table.status',       label: 'Státusz'},
];

function renderTasksHeader() {
    const head = el('tasksTableHead');
    if (!head) {
        return;
    }
    const sortConfig = state.tasksSort;
    const cols = TASK_HEADERS.map(({key, i18n, label}) => {
        const active = sortConfig.column === key;
        const indicator = active ? (sortConfig.dir === 'asc' ? '▲' : '▼') : '';
        return `<th class="sortable" data-action="sort-tasks" data-id="${key}" data-i18n="${i18n}">${escapeHtml(t(i18n) || label)}<span class="sort-indicator">${indicator}</span></th>`;
    }).join('');
    head.innerHTML = `<tr>${cols}<th data-i18n="table.actions">${escapeHtml(t('table.actions') || 'Műveletek')}</th></tr>`;
}

export function renderTasksTable(tasks) {
    const tbody = el('tasksTableBody');
    if (!tbody) {
        return;
    }

    renderTasksHeader();

    if (!tasks || tasks.length === 0) {
        tbody.innerHTML = `
        <tr>
            <td colspan="6" class="empty-state">
                ${escapeHtml(t('tasks.empty'))}
            </td>
        </tr>`;
        renderPagination(0);
        return;
    }

    const ordered = prepareTasksForDisplay(tasks, state.tasksSort);
    const {pageItems, totalPages} = paginateItems(ordered, state.tasksPage, state.tasksPerPage);
    state.tasksPage = Math.min(state.tasksPage, totalPages);

    tbody.innerHTML = pageItems
        .map(renderTaskRow)
        .join('');

    renderPagination(totalPages);
}

/**
 * Pure data transformation (no DOM access): arranges recurring task groups side by side,
 * then sorts them by the selected column/direction while keeping each group together.
 * Kept separate from renderTasksTable() to make it independently testable and reusable
 * (e.g. for future unit tests without a DOM environment).
 */
export function prepareTasksForDisplay(tasks, sortConfig) {
    const grouped = groupRecurringTasks(tasks);
    return sortTasksKeepingGroupsTogether(grouped, sortConfig.column, sortConfig.dir);
}

// Recurring task series ID, or null if the task does not belong to a series.
function getRecurrenceGroupKey(task) {
    if (task.recurrenceRootId) {
        return String(task.recurrenceRootId);
    }
    if (task.recurrenceUnit && task.recurrenceUnit !== 'NONE') {
        return String(task.id);
    }
    return null;
}

// Groups arranged side by side (sorted by start time within each group), followed by non-recurring tasks.
function groupRecurringTasks(tasks) {
    const byGroup = new Map();
    const singles = [];

    tasks.forEach((task) => {
        const groupKey = getRecurrenceGroupKey(task);
        if (!groupKey) {
            singles.push(task);
            return;
        }
        if (!byGroup.has(groupKey)) {
            byGroup.set(groupKey, []);
        }
        byGroup.get(groupKey).push(task);
    });

    const grouped = [];
    byGroup.forEach((items) => {
        items.sort((a, b) => compareValues(a.startDate, b.startDate, 'date'));
        grouped.push(...items);
    });

    return grouped.concat(singles);
}

/**
 * Sorts by the selected column/direction, but between different recurring task groups,
 * the group ID always determines the order (not the selected column), ensuring that
 * rows from the same series are never split apart by other tasks.
 */
function sortTasksKeepingGroupsTogether(tasks, column, dir) {
    const multiplier = dir === 'asc' ? 1 : -1;
    const type = column === 'startDate' ? 'date' : 'string';

    return tasks.slice().sort((a, b) => {
        const aGroup = getRecurrenceGroupKey(a);
        const bGroup = getRecurrenceGroupKey(b);
        if (aGroup && bGroup && aGroup !== bGroup) {
            return compareValues(aGroup, bGroup, 'string');
        }
        return multiplier * compareValues(a[column], b[column], type);
    });
}

// Extracts one page of items and calculates the valid page number and total page count.
export function paginateItems(items, page, perPage) {
    const totalPages = Math.max(1, Math.ceil(items.length / perPage));
    const currentPage = Math.min(Math.max(1, page), totalPages);
    const startIdx = (currentPage - 1) * perPage;
    return {
        pageItems: items.slice(startIdx, startIdx + perPage),
        totalPages,
    };
}

function renderTaskRow(task) {
    const title = escapeHtml(task.title || '');
    const description = escapeHtml(task.description || '-');
    const priorityClass = String(task.priority || '').toLowerCase();
    const statusClass = String(task.status || '').toLowerCase();
    const priorityLabel = escapeHtml(getPriorityLabel(task.priority));
    const dateRange = escapeHtml(formatTaskRange(task, t('task.noDates')));
    const statusLabel = escapeHtml(getStatusLabel(task.status));

    const recurring = task.recurrenceRootId || (task.recurrenceUnit && task.recurrenceUnit !== 'NONE');
    const rowClass = recurring ? 'recurring-group' : '';
    const recurringBadge = recurring ? `<span class="recurring-badge" title="${escapeHtml(t('recurring.group'))}">•</span>` : '';

    return `
    <tr class="${rowClass}">
      <td>
        <strong>
          <span class="task-title" title="${title}">
            ${recurringBadge} ${title}
          </span>
        </strong>
      </td>
      <td>
        <span class="task-desc" title="${description}" >
          ${description}
        </span>
      </td>
      <td>
        ${dateRange}
      </td>
      <td>
        <span class="priority-${priorityClass}">
          ${priorityLabel}
        </span>
      </td>
      <td>
        <span class="status-${statusClass}">
          ${statusLabel}
        </span>
      </td>
      <td>
        ${renderTaskActions(task.id)}
      </td>
    </tr>
  `;
}

function renderTaskActions(taskId) {
    return `
    <div class="task-actions">
      <button class="btn-action btn-edit" data-action="edit-task" data-id="${taskId}">
        ${escapeHtml(t('action.edit'))}
      </button>
      <button class="btn-action btn-delete" data-action="delete-task" data-id="${taskId}">
        ${escapeHtml(t('action.delete'))}
      </button>
    </div>
  `;
}

function renderPagination(totalPages) {
    const container = el('tasksPagination');
    if (!container) return;
    const current = state.tasksPage || 1;
    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    const prevDisabled = current <= 1 ? 'disabled' : '';
    const nextDisabled = current >= totalPages ? 'disabled' : '';

    // Build page buttons with windowing
    const pages = [];
    const maxButtons = 7; // show up to 7 numbered buttons
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

    const pageButtons = pages.map(p => {
        if (p === 'left-ellipsis' || p === 'right-ellipsis') return `<span class="pagination-ellipsis">&hellip;</span>`;
        const active = p === current ? 'pagination-page active' : 'pagination-page';
        return `<button class="btn-secondary ${active}" data-action="change-page" data-id="${p}">${p}</button>`;
    }).join('');

    container.innerHTML = `
        <button class="btn-secondary" ${prevDisabled} data-action="change-page" data-id="${current - 1}">&laquo;</button>
        ${pageButtons}
        <button class="btn-secondary" ${nextDisabled} data-action="change-page" data-id="${current + 1}">&raquo;</button>
    `;
}

export function getTaskById(taskId) {
    return state.tasks.find((task) => String(task.id) === String(taskId)) || null;
}

export function beginTaskEditing(task) {
    el('taskId').value = task.id;
    populateTaskFields('task', task);
    state.editingTaskId = task.id;
    updateTaskFormMode();
    el('taskTitle').focus();
}

export function startEdit(taskId) {
    const task = getTaskById(taskId);
    if (!task) {
        showAppMessage(t('task.notFound', {id: taskId}), true);
        return;
    }
    beginTaskEditing(task);
    showAppMessage(t('editing.started'), false);
    window.scrollTo({top: 0, behavior: 'smooth'});
}

// Builds the backend payload from taskForm fields. The username comes from the JWT, not the payload.
export function buildTaskPayload() {
    return readTaskFieldsPayload('task');
}

export async function saveTask(event) {
    event.preventDefault();

    if (!state.token) {
        handleUnauthorized();
        return;
    }
    if (state.role === 'ADMIN') {
        showAppMessage(t('messages.adminReadOnly'), true);
        return;
    }

    const payload = buildTaskPayload();
    const editing = Boolean(state.editingTaskId);

    if (!payload.title || !payload.priority || !payload.status || !payload.startDate) {
        showAppMessage(t('messages.taskFieldsRequired'), true);
        return;
    }
    if (payload.endDate && new Date(payload.endDate) < new Date(payload.startDate)) {
        showAppMessage(t('messages.invalidDateRange'), true);
        return;
    }

    try {
        const response = await fetch(editing ? `/api/tasks/${state.editingTaskId}` : '/api/tasks', {
            method: editing ? 'PUT' : 'POST',
            headers: authHeaders({'Content-Type': 'application/json'}),
            body: JSON.stringify(payload),
        });
        if (response.status === 401 || response.status === 403) {
            handleUnauthorized();
            return;
        }
        if (!response.ok) {
            showAppMessage(t('messages.taskSaveFailed'), true);
            return;
        }

        showAppMessage(editing
            ? t('messages.taskUpdateSuccess')
            : t('messages.taskCreateSuccess'), false);

        el('taskForm').reset();
        resetTaskDefaults();
        await loadTasks();
    } catch (err) {
        showAppMessage(`${t('messages.taskSaveFailed')}: ${err.message}`, true);
    }
}

export async function deleteTask(taskId) {
    if (!confirm(t('task.deleteConfirm'))) {
        return;
    }
    if (!state.token) {
        handleUnauthorized();
        return;
    }
    if (state.role === 'ADMIN') {
        showAppMessage(t('messages.adminReadOnly'), true);
        return;
    }

    try {
        const response = await fetch(`/api/tasks/${taskId}`, {
            method: 'DELETE',
            headers: authHeaders(),
        });

        if (response.status === 401 || response.status === 403) {
            handleUnauthorized();
            return;
        }
        if (!response.ok) {
            showAppMessage(t('messages.taskDeleteFailed'), true);
            return;
        }

        showAppMessage(t('messages.taskDeleteSuccess'), false);
        await loadTasks();
    } catch (err) {
        showAppMessage(`${t('messages.taskDeleteFailed')}: ${err.message}`, true);
    }
}

export function sortTasksBy(column) {
    if (state.tasksSort.column === column) {
        state.tasksSort.dir = state.tasksSort.dir === 'asc' ? 'desc' : 'asc';
    } else {
        state.tasksSort.column = column;
        state.tasksSort.dir = 'asc';
    }
    renderTasksTable(state.tasks);
}

export function changeTasksPage(page) {
    state.tasksPage = Math.max(1, page);
    renderTasksTable(state.tasks);
}

/**
 * Sets up event listeners once for the task table, pagination, and calendar using event delegation
 * (see dom-events.js). Called once during app.js initialization - innerHTML re-renders do not affect
 * the bindings because the listener is attached to the stable parent container, not individual buttons.
 */
export function initTaskEvents() {
    bindActions(el('tasksTable'), {
        'edit-task': (id) => startEdit(Number(id)),
        'delete-task': (id) => deleteTask(Number(id)),
        'sort-tasks': (column) => sortTasksBy(column),
    });
    bindActions(el('tasksPagination'), {
        'change-page': (id) => changeTasksPage(Number(id)),
    });
    if (elExists('calendarView')) {
        bindActions(el('calendarView'), {
            'edit-task': (id) => startEdit(Number(id)),
        });
    }
}