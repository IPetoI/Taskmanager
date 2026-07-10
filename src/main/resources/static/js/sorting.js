/**
 * Shared sorting/comparison utility functions.
 * Previously, admin.js and tasks.js implemented the same functionality independently
 * with slightly different logic. This module is the single source of truth for both.
 */

/**
 * Compares two values based on their type. `null`/`undefined` values are always placed
 * at the end of the list, regardless of the sort direction (asc/desc). The caller
 * multiplies the result by +1/-1 to apply the desired direction.
 *
 * @param {*} a
 * @param {*} b
 * @param {'string'|'number'|'date'} type
 * @param {string} locale - e.g. 'hu' or 'en', used for string comparisons
 */
export function compareValues(a, b, type = 'string', locale = 'hu') {
    if (a === null || a === undefined || a === '') {
        return (b === null || b === undefined || b === '') ? 0 : 1;
    }
    if (b === null || b === undefined || b === '') {
        return -1;
    }

    if (type === 'number') {
        return Number(a) - Number(b);
    }

    if (type === 'date') {
        const aTime = new Date(a).getTime();
        const bTime = new Date(b).getTime();
        if (Number.isNaN(aTime) && Number.isNaN(bTime)) return 0;
        if (Number.isNaN(aTime)) return 1;
        if (Number.isNaN(bTime)) return -1;
        return aTime - bTime;
    }

    return String(a).localeCompare(String(b), locale, {sensitivity: 'base'});
}

/**
 * Sorts a copy of the `items` array by the specified column and direction.
 * `typeByColumn` is a `{columnName: 'number'|'date'|'string'}` map that defines
 * how each column should be compared (default: 'string').
 */
export function sortByColumn(items, column, dir, typeByColumn = {}, locale = 'hu') {
    const multiplier = dir === 'desc' ? -1 : 1;
    const type = typeByColumn[column] || 'string';

    return items.slice().sort((a, b) => multiplier * compareValues(a[column], b[column], type, locale));
}
