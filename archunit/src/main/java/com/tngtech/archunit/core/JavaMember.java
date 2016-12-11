package com.tngtech.archunit.core;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.core.Guava.toGuava;
import static com.tngtech.archunit.core.JavaAnnotation.GET_TYPE_NAME;

public abstract class JavaMember<M extends Member, T extends MemberDescription<M>>
        implements HasName.AndFullName, HasOwner.IsOwnedByClass, HasDescriptor {

    private final String name;
    private final String descriptor;
    private final Map<String, JavaAnnotation> annotations;
    private final JavaClass owner;
    private final Set<JavaModifier> modifiers;

    JavaMember(JavaClass owner, Set<JavaAnnotation> annotations, String name, String descriptor, Set<JavaModifier> modifiers) {
        this.name = checkNotNull(name);
        this.descriptor = checkNotNull(descriptor);
        this.annotations = FluentIterable.from(annotations).uniqueIndex(toGuava(GET_TYPE_NAME));
        this.owner = checkNotNull(owner);
        this.modifiers = checkNotNull(modifiers);
    }

    public Set<JavaAnnotation> getAnnotations() {
        return ImmutableSet.copyOf(annotations.values());
    }

    /**
     * Returns the {@link JavaAnnotation} of this field for the given {@link java.lang.annotation.Annotation} type.
     *
     * @throws IllegalArgumentException if there is no annotation of the respective reflection type
     */
    public JavaAnnotation getAnnotationOfType(Class<?> type) {
        return tryGetAnnotationOfType(type).getOrThrow(new IllegalArgumentException(String.format(
                "Member %s is not annotated with @%s",
                getFullName(), type.getSimpleName())));
    }

    public Optional<JavaAnnotation> tryGetAnnotationOfType(Class<?> type) {
        return Optional.fromNullable(annotations.get(type.getName()));
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    public Set<JavaModifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    public abstract Set<? extends JavaAccess<?>> getAccessesToSelf();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{member=" + name + ", owner=" + getOwner() + '}';
    }

    public static DescribedPredicate<JavaMember<?, ?>> modifier(final JavaModifier modifier) {
        return new DescribedPredicate<JavaMember<?, ?>>("modifier " + modifier) {
            @Override
            public boolean apply(JavaMember<?, ?> input) {
                return input.getModifiers().contains(modifier);
            }
        };
    }

    public static final ChainableFunction<HasOwner<JavaClass>, JavaClass> GET_OWNER =
            new ChainableFunction<HasOwner<JavaClass>, JavaClass>() {
                @Override
                public JavaClass apply(HasOwner<JavaClass> input) {
                    return input.getOwner();
                }
            };

    static abstract class Builder<RAW extends MemberDescription<?>, OUTPUT> implements BuilderWithBuildParameter<JavaClass, OUTPUT> {
        RAW member;
        JavaClass owner;

        Builder<RAW, OUTPUT> withMember(RAW member) {
            this.member = member;
            return this;
        }
    }
}
