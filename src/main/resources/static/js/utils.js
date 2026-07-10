import {state} from './state.js';


// Maximum number of months the calendar can navigate before and after the current month.
const CALENDAR_RANGE_MONTHS = 18;


export const el = (id) => document.getElementById(id);

export function elExists(id) {
    return Boolean(el(id));
}

export function startOfDay(date) {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
}

// Returns the Monday of the week containing the given date, set to 00:00.
export function startOfWeek(date) {
    const d = startOfDay(date);
    const day = d.getDay();
    const mondayIndex = day === 0 ? 6 : day - 1;
    d.setDate(d.getDate() - mondayIndex);
    return d;
}

export function addDays(date, days) {
    const d = new Date(date);
    d.setDate(d.getDate() + days);
    return d;
}

export function addHours(date, hours) {
    const d = new Date(date);
    d.setHours(d.getHours() + hours);
    return d;
}

export function addMonths(date, months) {
    const d = new Date(date);
    d.setMonth(d.getMonth() + months);
    return d;
}

// Clamps the date to the inclusive [minDate, maxDate] range.
export function clampDate(date, minDate, maxDate) {
    const time = date.getTime();
    if (time < minDate.getTime()) {
        return new Date(minDate);
    }
    if (time > maxDate.getTime()) {
        return new Date(maxDate);
    }
    return new Date(date);
}

export function getCalendarMinDate() {
    return startOfDay(addMonths(new Date(), -CALENDAR_RANGE_MONTHS));
}

export function getCalendarMaxDate() {
    const d = addMonths(new Date(), CALENDAR_RANGE_MONTHS);
    d.setHours(23, 59, 59, 999);
    return d;
}

// Converts any date-like value to a Date object, or returns null for invalid or missing input.
export function toDate(value) {
    if (!value) {
        return null;
    }
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? null : d;
}

// Formats a Date object as "YYYY-MM-DDTHH:mm" for an <input type="datetime-local"> element.
export function toDatetimeLocal(value) {
    const d = value ? toDate(value) : null;
    if (!d) {
        return '';
    }
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDateTime(value) {
    const d = toDate(value);
    if (!d) {
        return '';
    }
    return new Intl.DateTimeFormat(
        state.language === 'en' ? 'en-US' : 'hu-HU', {dateStyle: 'medium', timeStyle: 'short'}
    ).format(d);
}

export function formatDateOnly(value) {
    const d = toDate(value);
    if (!d) {
        return '';
    }
    return new Intl.DateTimeFormat(
        state.language === 'en' ? 'en-US' : 'hu-HU', {month: 'short', day: '2-digit', year: 'numeric'}
    ).format(d);
}

export function formatTimeOnly(value) {
    const d = toDate(value);
    if (!d) {
        return '';
    }
    return new Intl.DateTimeFormat(
        state.language === 'en' ? 'en-US' : 'hu-HU', {hour: '2-digit', minute: '2-digit'}
    ).format(d);
}

export function formatCalendarMonthLabel(date) {
    return new Intl.DateTimeFormat(
        state.language === 'en' ? 'en-US' : 'hu-HU', {month: 'long', year: 'numeric'}
    ).format(date);
}

// Returns the date range displayed in the task table/event card, or a fallback if no start date is available.
export function formatTaskRange(task, noDatesLabel = 'Nincs dátum') {
    const start = toDate(task.startDate);
    const end = toDate(task.endDate);
    if (!start) {
        return noDatesLabel;
    }
    return end ? `${formatDateTime(start)} - ${formatDateTime(end)}` : formatDateTime(start);
}

// Default start time for a new task, rounded up to the next 15-minute interval.
export function defaultTaskStart() {
    const d = new Date();
    const SLOT = 15;
    d.setSeconds(0, 0);
    const minutes = d.getMinutes();
    const rounded = Math.ceil(minutes / SLOT) * SLOT;

    if (rounded === 60) {
        d.setHours(d.getHours() + 1, 0);
    } else {
        d.setMinutes(rounded);
    }

    return d;
}

export function defaultTaskEnd(start = defaultTaskStart()) {
    const ONE_HOUR = 60 * 60 * 1000;
    return new Date(start.getTime() + ONE_HOUR);
}

// Escapes text for safe HTML rendering to prevent XSS when injecting dynamic content.
export function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = (text === null || text === undefined) ? '' : text;
    return div.innerHTML;
}

export function sameDay(a, b) {
    return a && b &&
        a.getFullYear() === b.getFullYear() &&
        a.getMonth() === b.getMonth() &&
        a.getDate() === b.getDate();
}

// Returns true if the task overlaps with the one-hour calendar slot (slotStart .. slotStart + 1h).
export function taskOverlapsSlot(task, slotStart) {
    const start = toDate(task.startDate);
    const end = toDate(task.endDate) || start;
    if (!start || !end) {
        return false;
    }
    const slotEnd = addHours(slotStart, 1);
    return start < slotEnd && end >= slotStart;
}

// Highlight color for the task status (calendar event side bar and background tint).
export function getTaskAccent(task) {
    const status = String(task.status || 'TODO').toUpperCase();
    const accents = {
        TODO: '#e74c3c',
        IN_PROGRESS: '#f39c12',
        DONE: '#27ae60',
    };
    return accents[status] || accents.TODO;
}