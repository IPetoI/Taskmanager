package com.ipetoi.taskmanager.model;

import java.time.LocalDateTime;

/**
 * Simple recurrence units (daily, weekly, monthly, yearly). NONE means no recurrence.
 * <p>
 * The {@code advance} and {@code horizon} methods contain the recurrence logic directly
 * in the enum, so callers (e.g., {@code TaskServiceImpl}) do not need to use a
 * {@code switch} statement for each unit. Adding a new unit only requires changes here.
 */
public enum RecurrenceUnit {

    NONE {
        @Override
        public LocalDateTime advance(LocalDateTime date, int interval) {
            return date;
        }

        @Override
        public LocalDateTime horizon(LocalDateTime base) {
            return null;
        }
    },

    DAILY {
        @Override
        public LocalDateTime advance(LocalDateTime date, int interval) {
            return date.plusDays(interval);
        }

        @Override
        public LocalDateTime horizon(LocalDateTime base) {
            return base.plusMonths(6);
        }
    },

    WEEKLY {
        @Override
        public LocalDateTime advance(LocalDateTime date, int interval) {
            return date.plusWeeks(interval);
        }

        @Override
        public LocalDateTime horizon(LocalDateTime base) {
            return base.plusYears(1);
        }
    },

    MONTHLY {
        @Override
        public LocalDateTime advance(LocalDateTime date, int interval) {
            return date.plusMonths(interval);
        }

        @Override
        public LocalDateTime horizon(LocalDateTime base) {
            return base.plusYears(2);
        }
    },

    YEARLY {
        @Override
        public LocalDateTime advance(LocalDateTime date, int interval) {
            return date.plusYears(interval);
        }

        @Override
        public LocalDateTime horizon(LocalDateTime base) {
            return base.plusYears(10);
        }
    };

    /**
     * Advances the given date according to the recurrence unit and interval.
     *
     * @param date     starting date (must not be null)
     * @param interval recurrence frequency (e.g., 1 = every unit, 2 = every second unit)
     */
    public abstract LocalDateTime advance(LocalDateTime date, int interval);


    // Returns the date limit up to which recurring instances should be generated.
    public abstract LocalDateTime horizon(LocalDateTime base);
}