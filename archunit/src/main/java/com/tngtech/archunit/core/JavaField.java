package com.tngtech.archunit.core;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class JavaField extends JavaMember<Field, MemberDescription.ForField> {
    private Set<JavaFieldAccess> accessesToSelf = Collections.emptySet();

    private JavaField(Builder builder) {
        super(builder.member, builder.owner);
    }

    @Override
    public String getFullName() {
        return getOwner().getName() + "." + getName();
    }

    public Class<?> getType() {
        return memberDescription.getType();
    }

    @Override
    public Set<JavaFieldAccess> getAccessesToSelf() {
        return accessesToSelf;
    }

    void registerAccessesToField(Set<JavaFieldAccess> accesses) {
        this.accessesToSelf = ImmutableSet.copyOf(accesses);
    }

    public static DescribedPredicate<JavaField> hasType(DescribedPredicate<? super Class<?>> predicate) {
        return predicate.onResultOf(GET_TYPE)
                .as("has type " + predicate.getDescription());
    }

    public static final Function<JavaField, Class<?>> GET_TYPE = new Function<JavaField, Class<?>>() {
        @Override
        public Class<?> apply(JavaField input) {
            return input.getType();
        }
    };

    static final class Builder extends JavaMember.Builder<MemberDescription.ForField, JavaField> {
        @Override
        public JavaField build(JavaClass owner) {
            this.owner = owner;
            return new JavaField(this);
        }

        BuilderWithBuildParameter<JavaClass, JavaField> withField(Field field) {
            return withMember(new MemberDescription.ForDeterminedField(field));
        }
    }
}
