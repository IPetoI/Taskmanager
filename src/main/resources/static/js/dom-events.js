/**
 * Event delegation helper.
 *
 * Previously, buttons generated inside table rows and calendar elements used inline
 * onclick="window.TaskApp.xyz(...)" attributes to call module functions through the
 * global window object.
 *
 * This approach had several drawbacks: global namespace pollution, misspelled action
 * names only being detected at runtime, and the need to handle escaping separately
 * for every generated button inside template strings.
 *
 * Instead, generated HTML elements only receive `data-action`/`data-id` attributes,
 * and a single delegated click listener is attached to a stable parent container.
 * The container itself is never replaced - only its contents are updated, so rewriting
 * innerHTML does not affect the attached event listener.
 */

/**
 * Attaches a single click listener to the `container`. When a click occurs on an
 * element (or one of its descendants) with a `[data-action]` attribute, it calls the
 * corresponding `handlers[action]` function with the `data-id` value.
 *
 * @param {HTMLElement|null} container
 * @param {Record<string, (id: string, event: MouseEvent) => void>} handlers
 */
export function bindActions(container, handlers) {
    if (!container) {
        return;
    }
    container.addEventListener('click', (event) => {
        const trigger = event.target.closest('[data-action]');
        if (!trigger || !container.contains(trigger)) {
            return;
        }
        const handler = handlers[trigger.dataset.action];
        if (handler) {
            handler(trigger.dataset.id, event);
        }
    });
}