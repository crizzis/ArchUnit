package com.tngtech.archunit.core;

import java.util.Objects;

import com.tngtech.archunit.core.HasOwner.IsOwnedByMethod;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class JavaAccess<TARGET extends HasName.AndFullName & HasOwner<JavaClass>>
        implements HasName, IsOwnedByMethod, HasDescription {

    private static final String LOCATION_TEMPLATE = "(%s.java:%d)";

    private final JavaMethodLike<?, ?> origin;
    private final TARGET target;
    private final int lineNumber;
    private final int hashCode;

    JavaAccess(AccessRecord<TARGET> record) {
        this(record.getCaller(), record.getTarget(), record.getLineNumber());
    }

    JavaAccess(JavaMethodLike<?, ?> origin, TARGET target, int lineNumber) {
        this.origin = checkNotNull(origin);
        this.target = checkNotNull(target);
        this.lineNumber = lineNumber;
        this.hashCode = Objects.hash(origin.getFullName(), target.getFullName(), lineNumber);
    }

    @Override
    public String getName() {
        return target.getName();
    }

    public JavaMethodLike<?, ?> getOrigin() {
        return origin;
    }

    public JavaClass getOriginClass() {
        return getOrigin().getOwner();
    }

    public TARGET getTarget() {
        return target;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public JavaMethodLike<?, ?> getOwner() {
        return getOrigin();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JavaAccess<?> other = (JavaAccess<?>) obj;
        return Objects.equals(this.origin.getFullName(), other.origin.getFullName()) &&
                Objects.equals(this.target.getFullName(), other.target.getFullName()) &&
                Objects.equals(this.lineNumber, other.lineNumber);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{origin=" + origin + ", target=" + target + ", lineNumber=" + lineNumber + additionalToStringFields() + '}';
    }

    String additionalToStringFields() {
        return "";
    }

    @Override
    public String getDescription() {
        return getDescriptionWithTemplate(descriptionTemplate());
    }

    public String getDescriptionWithTemplate(String template) {
        String description = String.format(template, getOwner().getFullName(), getTarget().getFullName());
        String location = String.format(LOCATION_TEMPLATE, getLocationClass().getSimpleName(), getLineNumber());
        return String.format("%s in %s", description, location);
    }

    private JavaClass getLocationClass() {
        JavaClass location = getOriginClass();
        while (location.getEnclosingClass().isPresent()) {
            location = location.getEnclosingClass().get();
        }
        return location;
    }

    protected abstract String descriptionTemplate();
}
