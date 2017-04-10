package com.tngtech.archunit.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.DomainObjectCreationContext.AccessContext;
import com.tngtech.archunit.core.importer.DomainBuilders;
import com.tngtech.archunit.core.properties.HasParameterTypes;
import com.tngtech.archunit.core.properties.HasReturnType;

import static com.tngtech.archunit.core.Formatters.formatMethod;

/**
 * Represents a unit of code containing accesses to other units of code. A unit of code can be
 * <ul>
 * <li>a method</li>
 * <li>a constructor</li>
 * <li>a static initializer</li>
 * </ul>
 * in particular every place, where Java code with behavior, like calling other methods or accessing fields, can
 * be defined.
 */
public abstract class JavaCodeUnit extends JavaMember implements HasParameterTypes, HasReturnType {
    private final JavaClass returnType;
    private final List<JavaClass> parameters;
    private final String fullName;

    private Set<JavaFieldAccess> fieldAccesses = Collections.emptySet();
    private Set<JavaMethodCall> methodCalls = Collections.emptySet();
    private Set<JavaConstructorCall> constructorCalls = Collections.emptySet();

    JavaCodeUnit(DomainBuilders.JavaCodeUnitBuilder<?, ?> builder) {
        super(builder);
        this.returnType = builder.getReturnType();
        this.parameters = builder.getParameters();
        fullName = formatMethod(getOwner().getName(), getName(), getParameters());
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public JavaClassList getParameters() {
        return new JavaClassList(parameters);
    }

    @Override
    public JavaClass getReturnType() {
        return returnType;
    }

    public Set<JavaFieldAccess> getFieldAccesses() {
        return fieldAccesses;
    }

    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        return methodCalls;
    }

    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        return constructorCalls;
    }

    public boolean isConstructor() {
        return false;
    }

    AccessContext.Part completeFrom(ImportContext context) {
        fieldAccesses = context.getFieldAccessesFor(this);
        methodCalls = context.getMethodCallsFor(this);
        constructorCalls = context.getConstructorCallsFor(this);

        return new AccessContext.Part(this);
    }

    @ResolvesTypesViaReflection
    @MayResolveTypesViaReflection(reason = "Just part of a bigger resolution procecss")
    static Class<?>[] reflect(JavaClassList parameters) {
        List<Class<?>> result = new ArrayList<>();
        for (JavaClass parameter : parameters) {
            result.add(parameter.reflect());
        }
        return result.toArray(new Class<?>[result.size()]);
    }

    public static class Predicates {
        public static DescribedPredicate<JavaCodeUnit> constructor() {
            return new DescribedPredicate<JavaCodeUnit>("constructor") {
                @Override
                public boolean apply(JavaCodeUnit input) {
                    return input.isConstructor();
                }
            };
        }
    }

}
