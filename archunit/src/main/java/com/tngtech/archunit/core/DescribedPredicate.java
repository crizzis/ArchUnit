package com.tngtech.archunit.core;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A predicate holding a description.
 *
 * @param <T> The type of objects the predicate applies to
 */
public abstract class DescribedPredicate<T> {
    private String description;

    public abstract boolean apply(T input);

    public DescribedPredicate(String description, Object... params) {
        checkArgument(description != null && !description.trim().isEmpty(), "Description must not be empty");
        this.description = String.format(description, params);
    }

    public String getDescription() {
        return description;
    }

    public DescribedPredicate<T> as(String description, Object... params) {
        return new DescribedPredicate<T>(description, params) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input);
            }
        };
    }

    public DescribedPredicate<T> and(final DescribedPredicate<T> other) {
        return new DescribedPredicate<T>(description + " and " + other.getDescription()) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input) && other.apply(input);
            }
        };
    }

    public DescribedPredicate<T> or(final DescribedPredicate<T> other) {
        return new DescribedPredicate<T>(description + " or " + other.getDescription()) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input) || other.apply(input);
            }
        };
    }

    public <F> DescribedPredicate<F> onResultOf(final Function<F, ? extends T> function) {
        checkNotNull(function);
        return new DescribedPredicate<F>(description) {
            @Override
            public boolean apply(F input) {
                return DescribedPredicate.this.apply(function.apply(input));
            }
        };
    }

    /**
     * This method is just syntactic sugar, e.g. to write aClass.that(is(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate
     */
    public static <T> DescribedPredicate<T> is(DescribedPredicate<T> predicate) {
        return predicate.as("is " + predicate.getDescription());
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(are(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate
     */
    public static <T> DescribedPredicate<T> are(DescribedPredicate<T> predicate) {
        return predicate.as("are " + predicate.getDescription());
    }

    public static <T> DescribedPredicate<T> alwaysTrue() {
        return new DescribedPredicate<T>("always true") {
            @Override
            public boolean apply(T input) {
                return true;
            }
        };
    }

    public static <T> DescribedPredicate<T> alwaysFalse() {
        return new DescribedPredicate<T>("always false") {
            @Override
            public boolean apply(T input) {
                return false;
            }
        };
    }

    public static <T> DescribedPredicate<T> equalTo(final T object) {
        checkNotNull(object);
        return new DescribedPredicate<T>("equal to '%s'", object) {
            @Override
            public boolean apply(T input) {
                return object.equals(input);
            }
        };
    }

    public static <T> DescribedPredicate<T> not(final DescribedPredicate<T> predicate) {
        checkNotNull(predicate);
        return new DescribedPredicate<T>("not " + predicate.getDescription()) {
            @Override
            public boolean apply(T input) {
                return !predicate.apply(input);
            }
        };
    }
}
