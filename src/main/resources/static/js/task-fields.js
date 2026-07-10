/**
 * Shared logic for reading from and populating task form fields.
 *
 * There are two similar task forms: the "New Task" form (ID prefix: "task",
 * e.g., #taskTitle) and the admin "Edit Task" form (ID prefix: "adminEditTask",
 * e.g., #adminEditTaskTitle). Although the HTML markup remains separate (different
 * cards, different submit buttons, and admin-specific fields), the logic for
 * reading and populating the form fields is identical. This module provides that
 * shared functionality in a single place.
 */
import {el, elExists, toDatetimeLocal} from './utils.js';

const FIELD_SUFFIX = {
    title: 'Title',
    description: 'Description',
    priority: 'Priority',
    status: 'Status',
    startDate: 'StartDate',
    endDate: 'EndDate',
    recurrenceUnit: 'RecurrenceUnit',
};

function fieldId(prefix, key) {
    return `${prefix}${FIELD_SUFFIX[key]}`;
}

// Populates the task form with the specified ID prefix (e.g., 'task' or 'adminEditTask') using the provided task data.
export function populateTaskFields(prefix, task) {
    el(fieldId(prefix, 'title')).value = task.title || '';
    el(fieldId(prefix, 'description')).value = task.description || '';
    el(fieldId(prefix, 'priority')).value = task.priority || 'MEDIUM';
    el(fieldId(prefix, 'status')).value = task.status || 'TODO';
    el(fieldId(prefix, 'startDate')).value = toDatetimeLocal(task.startDate);
    el(fieldId(prefix, 'endDate')).value = toDatetimeLocal(task.endDate);

    const recurrenceId = fieldId(prefix, 'recurrenceUnit');
    if (elExists(recurrenceId)) {
        el(recurrenceId).value = task.recurrenceUnit || 'NONE';
    }
}

// Builds a payload object to send to the backend from the task form fields with the specified ID prefix.
export function readTaskFieldsPayload(prefix) {
    const startDateRaw = el(fieldId(prefix, 'startDate')).value;
    const endDateRaw = el(fieldId(prefix, 'endDate')).value;
    const startDate = startDateRaw ? new Date(startDateRaw) : null;
    const endDate = endDateRaw ? new Date(endDateRaw) : null;

    const recurrenceId = fieldId(prefix, 'recurrenceUnit');

    return {
        title: el(fieldId(prefix, 'title')).value.trim(),
        description: el(fieldId(prefix, 'description')).value.trim(),
        priority: el(fieldId(prefix, 'priority')).value,
        status: el(fieldId(prefix, 'status')).value,
        startDate: startDate && !Number.isNaN(startDate.getTime()) ? startDateRaw : null,
        endDate: endDate && !Number.isNaN(endDate.getTime()) ? endDateRaw : null,
        recurrenceUnit: elExists(recurrenceId) ? (el(recurrenceId).value || 'NONE') : 'NONE',
    };
}