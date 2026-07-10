import {
    addDays, addMonths, clampDate, el, escapeHtml, formatCalendarMonthLabel, formatDateOnly,
    formatTimeOnly, getCalendarMaxDate, getCalendarMinDate, getTaskAccent, sameDay, startOfDay,
    startOfWeek, taskOverlapsSlot, toDate
} from './utils.js';
import {state} from './state.js';


function getWeekdayShortLabels() {
    const base = startOfWeek(new Date());
    const fmt = new Intl.DateTimeFormat(
        state.language === 'en' ? 'en-US' : 'hu-HU', {weekday: 'short'});
    return Array.from({length: 7}, (_, i) => fmt.format(addDays(base, i)));
}

// Only tasks with a valid start date are displayed in the calendar.
function getVisibleTasks() {
    return (state.tasks || []).filter((task) => toDate(task.startDate));
}

function renderEventCard(task, {compact = false} = {}) {
    const priorityClass = `priority-${String(task.priority || 'LOW').toLowerCase()}`;
    const statusClass = `status-${String(task.status || 'TODO').toLowerCase()}`;
    const accent = getTaskAccent(task);
    const time = task.endDate
        ? `${formatTimeOnly(task.startDate)} - ${formatTimeOnly(task.endDate)}`
        : formatTimeOnly(task.startDate);
    return `
    <div class="calendar-event ${priorityClass} ${statusClass} ${compact ? 'calendar-event-compact' : ''}" 
    style="--event-accent:${accent}" 
    data-action="edit-task" data-id="${task.id}" 
    title="${escapeHtml(task.title)}">
      <div class="event-title">${escapeHtml(task.title)}</div>
      <div class="event-time">${escapeHtml(time)}</div>
    </div>
  `;
}

function renderWeekCalendar(tasks) {
    const weekStart = startOfWeek(state.calendarCursor);
    const days = Array.from({length: 7}, (_, i) => addDays(weekStart, i));

    return `
    <table class="calendar-table">
      ${renderWeekHeader(days)}
      <tbody>
        ${renderWeekRows(days, tasks)}
      </tbody>
    </table>
  `;
}

function renderWeekHeader(days) {
    const weekdays = getWeekdayShortLabels();
    const today = startOfDay(new Date());

    const headers = days
        .map((day, i) => {
            const isToday = sameDay(day, today);
            const isWeekend = day.getDay() === 0 || day.getDay() === 6;

            return `
        <th class="
          calendar-day-head
          ${isToday ? 'calendar-day-today' : ''}
          ${isWeekend ? 'calendar-day-weekend' : ''}">
          <div>${escapeHtml(weekdays[i])}</div>
          <div>${escapeHtml(formatDateOnly(day))}</div>
        </th>
      `;
        })
        .join('');

    return `
    <thead>
      <tr>
        <th class="calendar-time-col">&nbsp;</th>
        ${headers}
      </tr>
    </thead>
  `;
}

function renderWeekRows(days, tasks) {
    let html = '';
    for (let hour = 0; hour < 24; hour += 1) {
        html += renderHourRow(hour, days, tasks);
    }
    return html;
}

function renderHourRow(hour, days, tasks) {
    const d = new Date();
    d.setHours(hour, 0, 0, 0);
    const label = new Intl.DateTimeFormat(
        state.language === 'en' ? 'en-US' : 'hu-HU',
        {hour: 'numeric', minute: '2-digit'}
    ).format(d);

    const slots = days
        .map((day) => renderWeekSlot(day, hour, tasks))
        .join('');

    return `
    <tr>
      <th class="calendar-time-col">
        ${label}
      </th>
      ${slots}
    </tr>
  `;
}

function renderWeekSlot(day, hour, tasks) {
    const today = startOfDay(new Date());
    const now = new Date();

    const slotStart = new Date(day);
    slotStart.setHours(hour, 0, 0, 0);

    const slotTasks = getTasksForSlot(tasks, slotStart);
    const slotAccent = slotTasks[0] ? getTaskAccent(slotTasks[0]) : '';
    const isToday = sameDay(day, today);
    const isWeekend = day.getDay() === 0 || day.getDay() === 6;

    const classes = getSlotClasses({
        isToday,
        isWeekend,
        hasTasks: slotTasks.length > 0,
    });

    return `
    <td class="${classes}"
      style="${slotAccent ? `--slot-accent:${slotAccent};` : ''}">
      <div class="calendar-slot">
        ${renderCurrentHourIndicator(isToday, hour, now)}
        ${renderSlotTasks(slotTasks)}
      </div>
    </td>
  `;
}

function getTasksForSlot(tasks, slotStart) {
    return tasks.filter((task) => taskOverlapsSlot(task, slotStart));
}

function getSlotClasses({isToday, isWeekend, hasTasks}) {
    return [
        !isToday && hasTasks ? 'calendar-slot--active' : '',
        isToday ? 'calendar-day-today' : '',
        isWeekend ? 'calendar-day-weekend' : '',
    ].filter(Boolean)
        .join(' ');
}

// Red horizontal line indicating the current time in today's time slot.
function renderCurrentHourIndicator(isToday, hour, now) {
    const currentHour = now.getHours();
    if (!isToday || hour !== currentHour) {
        return '';
    }
    const topPct = (now.getMinutes() / 60) * 100;

    return `
    <div class="current-hour-indicator"
      style="top:${topPct}%"></div>
  `;
}

function renderSlotTasks(tasks) {
    if (!tasks.length) {
        return '&nbsp;';
    }
    return tasks.map((task) =>
        renderEventCard(task, {
            compact: true,
        }))
        .join('');
}

function renderMonthCalendar(tasks) {
    const firstOfMonth = new Date(state.calendarCursor.getFullYear(), state.calendarCursor.getMonth(), 1);
    const gridStart = startOfWeek(firstOfMonth);

    return `
    <table class="calendar-table">
      ${renderCalendarHeader()}
      <tbody>
        ${renderCalendarWeeks(gridStart, firstOfMonth, tasks)}
      </tbody>
    </table>
  `;
}

function renderCalendarHeader() {
    const weekdays = getWeekdayShortLabels();

    const dayHeaders = weekdays
        .map((day, i) => {
            const isWeekend = i === 5 || i === 6;

            return `
        <th class="calendar-day-head
          ${isWeekend ? 'calendar-day-weekend' : ''}">
          ${escapeHtml(day)}
        </th>
      `;
        })
        .join('');

    return `
    <thead>
      <tr>
        <th class="calendar-time-col">&nbsp;</th>
        ${dayHeaders}
      </tr>
    </thead>
  `;
}

// The monthly view always renders a 6-week grid (42 days) to ensure the entire month
// fits consistently, regardless of the starting weekday or the number of weeks it spans.
const MONTH_GRID_WEEKS = 6;
const DAYS_PER_WEEK = 7;

function renderCalendarWeeks(gridStart, firstOfMonth, tasks) {
    let html = '';
    let current = new Date(gridStart);

    for (let week = 0; week < MONTH_GRID_WEEKS; week += 1) {
        html += '<tr><th class="calendar-time-col">&nbsp;</th>';

        for (let day = 0; day < DAYS_PER_WEEK; day += 1) {
            html += renderCalendarDayCell(
                current,
                firstOfMonth,
                tasks
            );
            current = addDays(current, 1);
        }
        html += '</tr>';
    }
    return html;
}

function renderCalendarDayCell(date, firstOfMonth, tasks) {
    const today = startOfDay(new Date());
    const inMonth = date.getMonth() === firstOfMonth.getMonth();
    const isToday = sameDay(date, today);
    const isWeekend = date.getDay() === 0 || date.getDay() === 6;
    const dayTasks = getTasksForDay(tasks, date);
    const dayAccent = dayTasks[0]
        ? getTaskAccent(dayTasks[0])
        : '';

    return `
    <td class="
        calendar-day-cell
        ${inMonth ? '' : 'calendar-day-outside'}
        ${isToday ? 'calendar-day-today' : ''}
        ${isWeekend ? 'calendar-day-weekend' : ''}"
      style="${dayAccent ? `--day-accent:${dayAccent};` : ''}">
      <div class="calendar-day-number">
        ${date.getDate()}
      </div>
      <div class="calendar-day-cells">
        ${renderDayTasks(dayTasks)}
      </div>
    </td>
  `;
}

// Active tasks for the given day (the day falls between the start and end dates),
// sorted by start time.
function getTasksForDay(tasks, date) {
    return tasks
        .filter((task) => {
            const start = toDate(task.startDate);
            const end = toDate(task.endDate) || start;

            return (
                start && end &&
                startOfDay(start) <= startOfDay(date) &&
                startOfDay(end) >= startOfDay(date)
            );
        }).sort(
            (a, b) =>
                (toDate(a.startDate)?.getTime() || 0) - (toDate(b.startDate)?.getTime() || 0)
        );
}

function renderDayTasks(tasks) {
    if (!tasks.length) {
        return `
      <div class="calendar-empty"></div>
    `;
    }
    return tasks
        .map(renderEventCard)
        .join('');
}

export function updateCalendarRangeLabel() {
    const label = el('calendarRangeLabel');
    if (!label) {
        return;
    }
    if (state.calendarView === 'week') {
        const start = startOfWeek(state.calendarCursor);
        const end = addDays(start, 6);
        label.textContent = `${formatDateOnly(start)} - ${formatDateOnly(end)}`;
    } else {
        label.textContent = formatCalendarMonthLabel(state.calendarCursor);
    }
}

export function renderCalendar() {
    const container = el('calendarView');
    if (!container) {
        return;
    }
    const tasks = getVisibleTasks();
    updateCalendarRangeLabel();
    container.innerHTML = tasks.length
        ? (state.calendarView === 'week' ? renderWeekCalendar(tasks) : renderMonthCalendar(tasks))
        : `<div class="calendar-empty"></div>`;
}

export function setCalendarView(view) {
    state.calendarView = view === 'month' ? 'month' : 'week';
    renderCalendar();
}

export function navigateCalendar(delta) {
    const min = getCalendarMinDate();
    const max = getCalendarMaxDate();
    const next = state.calendarView === 'month'
        ? addMonths(state.calendarCursor, delta)
        : addDays(state.calendarCursor, delta * 7);
    state.calendarCursor = clampDate(next, min, max);
    renderCalendar();
}

export function goToToday() {
    state.calendarCursor = new Date();
    renderCalendar();
}