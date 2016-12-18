package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.tngtech.archunit.core.BuilderWithBuildParameter.BuildFinisher.build;
import static com.tngtech.archunit.core.JavaAnnotation.buildAnnotations;
import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.ReflectionUtils.classForName;

public class JavaClass implements HasName {
    private final TypeDetails typeDetails;
    private final Set<JavaField> fields;
    private final Set<JavaCodeUnit> codeUnits;
    private final Set<JavaMethod> methods;
    private final Set<JavaConstructor> constructors;
    private final Optional<JavaStaticInitializer> staticInitializer;
    private Optional<JavaClass> superClass = Optional.absent();
    private final Set<JavaClass> interfaces = new HashSet<>();
    private final Set<JavaClass> subClasses = new HashSet<>();
    private Optional<JavaClass> enclosingClass = Optional.absent();
    private final Map<String, JavaAnnotation> annotations;

    private JavaClass(Builder builder) {
        typeDetails = checkNotNull(builder.typeDetails);
        fields = build(builder.fieldBuilders, this);
        methods = build(builder.methodBuilders, this);
        constructors = build(builder.constructorBuilders, this);
        staticInitializer = builder.staticInitializerBuilder.isPresent() ?
                Optional.of(builder.staticInitializerBuilder.get().build(this)) :
                Optional.<JavaStaticInitializer>absent();
        codeUnits = ImmutableSet.<JavaCodeUnit>builder()
                .addAll(methods).addAll(constructors).addAll(staticInitializer.asSet())
                .build();
        this.annotations = buildAnnotations(builder.annotations);
    }

    @Override
    public String getName() {
        return typeDetails.getName();
    }

    public String getSimpleName() {
        return typeDetails.getSimpleName();
    }

    public String getPackage() {
        return typeDetails.getPackage();
    }

    public boolean isInterface() {
        return typeDetails.isInterface();
    }

    public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
        return reflect().isAnnotationPresent(annotation);
    }

    /**
     * @param type The type of the {@link Annotation} to retrieve
     * @return The {@link Annotation} representing the given annotation type
     * @throws IllegalArgumentException if the class is note annotated with the given type
     * @see #isAnnotatedWith(Class)
     * @see #getAnnotation(Class)
     */
    public <A extends Annotation> A getReflectionAnnotation(Class<A> type) {
        return getAnnotation(type).as(type);
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return The {@link JavaAnnotation} representing the given annotation type
     * @throws IllegalArgumentException if the class is note annotated with the given type
     * @see #isAnnotatedWith(Class)
     * @see #tryGetAnnotation(Class)
     */
    public JavaAnnotation getAnnotation(Class<? extends Annotation> type) {
        return tryGetAnnotation(type).getOrThrow(new IllegalArgumentException(
                String.format("Type %s is not annotated with @%s", getSimpleName(), type.getSimpleName())));
    }

    /**
     * @param type A given annotation type to match {@link JavaAnnotation JavaAnnotations} against
     * @return An {@link Optional} containing a {@link JavaAnnotation} representing the given annotation type,
     * if this class is annotated with the given type, otherwise Optional.absent()
     * @see #isAnnotatedWith(Class)
     * @see #getAnnotation(Class)
     */
    public Optional<JavaAnnotation> tryGetAnnotation(Class<? extends Annotation> type) {
        return Optional.fromNullable(annotations.get(type.getName()));
    }

    public Optional<JavaClass> getSuperClass() {
        return superClass;
    }

    /**
     * @return The complete class hierarchy, i.e. the class itself and the result of {@link #getAllSuperClasses()}
     */
    public List<JavaClass> getClassHierarchy() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        result.add(this);
        result.addAll(getAllSuperClasses());
        return result.build();
    }

    /**
     * @return All super classes sorted ascending by distance in the class hierarchy, i.e. first the direct super class,
     * then the super class of the super class and so on. Includes Object.class in the result.
     */
    public List<JavaClass> getAllSuperClasses() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        JavaClass current = this;
        while (current.getSuperClass().isPresent()) {
            current = current.getSuperClass().get();
            result.add(current);
        }
        return result.build();
    }

    public Set<JavaClass> getSubClasses() {
        return subClasses;
    }

    public Set<JavaClass> getInterfaces() {
        return interfaces;
    }

    public Set<JavaClass> getAllInterfaces() {
        ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
        for (JavaClass i : interfaces) {
            result.add(i);
            result.addAll(i.getAllInterfaces());
        }
        if (superClass.isPresent()) {
            result.addAll(superClass.get().getAllInterfaces());
        }
        return result.build();
    }

    public Optional<JavaClass> getEnclosingClass() {
        return enclosingClass;
    }

    public Set<JavaClass> getAllSubClasses() {
        Set<JavaClass> result = new HashSet<>();
        for (JavaClass subClass : subClasses) {
            result.add(subClass);
            result.addAll(subClass.getAllSubClasses());
        }
        return result;
    }

    public Set<JavaField> getFields() {
        return fields;
    }

    public Set<JavaField> getAllFields() {
        ImmutableSet.Builder<JavaField> result = ImmutableSet.builder();
        for (JavaClass javaClass : getClassHierarchy()) {
            result.addAll(javaClass.getFields());
        }
        return result.build();
    }

    public JavaField getField(String name) {
        return tryGetField(name).getOrThrow(new IllegalArgumentException("No field with name '" + name + " in class " + getName()));
    }

    public Optional<JavaField> tryGetField(String name) {
        for (JavaField field : fields) {
            if (name.equals(field.getName())) {
                return Optional.of(field);
            }
        }
        return Optional.absent();
    }

    public Set<JavaCodeUnit> getCodeUnits() {
        return codeUnits;
    }

    /**
     * @param name       The name of the code unit, can be a method name, but also
     *                   {@link JavaConstructor#CONSTRUCTOR_NAME CONSTRUCTOR_NAME}
     *                   or {@link JavaStaticInitializer#STATIC_INITIALIZER_NAME STATIC_INITIALIZER_NAME}
     * @param parameters The parameter signature of the method
     * @return A code unit (method, constructor or static initializer) with the given signature
     */
    public JavaCodeUnit getCodeUnit(String name, TypeDetails... parameters) {
        return getCodeUnit(name, ImmutableList.copyOf(parameters));
    }

    public JavaCodeUnit getCodeUnit(String name, List<TypeDetails> parameters) {
        return findMatchingCodeUnit(codeUnits, name, parameters);
    }

    private <T extends JavaCodeUnit> T findMatchingCodeUnit(Set<T> codeUnits, String name, List<TypeDetails> parameters) {
        return tryFindMatchingCodeUnit(codeUnits, name, parameters).getOrThrow(new IllegalArgumentException("No code unit with name '" + name + "' and parameters " + parameters +
                " in codeUnits " + codeUnits + " of class " + getName()));
    }

    private <T extends JavaCodeUnit> Optional<T> tryFindMatchingCodeUnit(Set<T> codeUnits, String name, List<TypeDetails> parameters) {
        for (T codeUnit : codeUnits) {
            if (name.equals(codeUnit.getName()) && parameters.equals(codeUnit.getParameters())) {
                return Optional.of(codeUnit);
            }
        }
        return Optional.absent();
    }

    public JavaMethod getMethod(String name, Class<?>... parameters) {
        return findMatchingCodeUnit(methods, name, TypeDetails.allOf(parameters));
    }

    public Set<JavaMethod> getMethods() {
        return methods;
    }

    public Set<JavaMethod> getAllMethods() {
        ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
        for (JavaClass javaClass : concat(getClassHierarchy(), getAllInterfaces())) {
            result.addAll(javaClass.getMethods());
        }
        return result.build();
    }

    public JavaConstructor getConstructor(Class<?>... parameters) {
        return findMatchingCodeUnit(constructors, CONSTRUCTOR_NAME, TypeDetails.allOf(parameters));
    }

    public Set<JavaConstructor> getConstructors() {
        return constructors;
    }

    public Set<JavaConstructor> getAllConstructors() {
        ImmutableSet.Builder<JavaConstructor> result = ImmutableSet.builder();
        for (JavaClass javaClass : getClassHierarchy()) {
            result.addAll(javaClass.getConstructors());
        }
        return result.build();
    }

    public Optional<JavaStaticInitializer> getStaticInitializer() {
        return staticInitializer;
    }

    public Set<JavaAccess<?>> getAccessesFromSelf() {
        return Sets.union(getFieldAccessesFromSelf(), getCallsFromSelf());
    }

    /**
     * @return Set of all {@link JavaAccess} in the class hierarchy, as opposed to the accesses this class directly performs.
     */
    public Set<JavaAccess<?>> getAllAccessesFromSelf() {
        ImmutableSet.Builder<JavaAccess<?>> result = ImmutableSet.builder();
        for (JavaClass clazz : getClassHierarchy()) {
            result.addAll(clazz.getAccessesFromSelf());
        }
        return result.build();
    }

    public Set<JavaFieldAccess> getFieldAccessesFromSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getFieldAccesses());
        }
        return result.build();
    }

    /**
     * Returns all calls of this class to methods or constructors.
     *
     * @see #getMethodCallsFromSelf()
     * @see #getConstructorCallsFromSelf()
     */
    public Set<JavaCall<?>> getCallsFromSelf() {
        return Sets.union(getMethodCallsFromSelf(), getConstructorCallsFromSelf());
    }

    public Set<JavaMethodCall> getMethodCallsFromSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getMethodCallsFromSelf());
        }
        return result.build();
    }

    public Set<JavaConstructorCall> getConstructorCallsFromSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaCodeUnit codeUnit : codeUnits) {
            result.addAll(codeUnit.getConstructorCallsFromSelf());
        }
        return result.build();
    }

    public Set<Dependency> getDirectDependencies() {
        Set<Dependency> result = new HashSet<>();
        for (JavaAccess<?> access : filterTargetNotSelf(getFieldAccessesFromSelf())) {
            result.add(Dependency.from(access));
        }
        for (JavaAccess<?> call : filterTargetNotSelf(getCallsFromSelf())) {
            result.add(Dependency.from(call));
        }
        return result;
    }

    private Set<JavaAccess<?>> filterTargetNotSelf(Set<? extends JavaAccess<?>> accesses) {
        Set<JavaAccess<?>> result = new HashSet<>();
        for (JavaAccess<?> access : accesses) {
            if (!access.getTarget().getOwner().equals(this)) {
                result.add(access);
            }
        }
        return result;
    }

    public Set<JavaMethodCall> getMethodCallsToSelf() {
        ImmutableSet.Builder<JavaMethodCall> result = ImmutableSet.builder();
        for (JavaMethod method : methods) {
            result.addAll(method.getCallsOfSelf());
        }
        return result.build();
    }

    public Set<JavaConstructorCall> getConstructorCallsToSelf() {
        ImmutableSet.Builder<JavaConstructorCall> result = ImmutableSet.builder();
        for (JavaConstructor constructor : constructors) {
            result.addAll(constructor.getCallsOfSelf());
        }
        return result.build();
    }

    public Set<JavaAccess<?>> getAccessesToSelf() {
        return ImmutableSet.<JavaAccess<?>>builder()
                .addAll(getFieldAccessesToSelf())
                .addAll(getMethodCallsToSelf())
                .addAll(getConstructorCallsToSelf())
                .build();
    }

    public Class<?> reflect() {
        return classForName(getName());
    }

    void completeClassHierarchyFrom(ImportContext context) {
        completeSuperClassFrom(context);
        completeInterfacesFrom(context);
    }

    private void completeSuperClassFrom(ImportContext context) {
        superClass = findClass(typeDetails.getSuperclass(), context);
        if (superClass.isPresent()) {
            superClass.get().subClasses.add(this);
        }
    }

    private void completeInterfacesFrom(ImportContext context) {
        for (TypeDetails i : typeDetails.getInterfaces()) {
            interfaces.add(findClass(i, context));
        }
        for (JavaClass i : interfaces) {
            i.subClasses.add(this);
        }
    }

    private static JavaClass findClass(TypeDetails type, ImportContext context) {
        return context.getJavaClassWithType(type.getName());
    }

    private static Optional<JavaClass> findClass(Optional<TypeDetails> type, ImportContext context) {
        return type.isPresent() ? Optional.of(findClass(type.get(), context)) : Optional.<JavaClass>absent();
    }

    CompletionProcess completeFrom(ImportContext context) {
        enclosingClass = findClass(typeDetails.getEnclosingClass(), context);
        return new CompletionProcess();
    }

    @Override
    public String toString() {
        return "JavaClass{name='" + typeDetails.getName() + "\'}";
    }

    public Set<JavaFieldAccess> getFieldAccessesToSelf() {
        ImmutableSet.Builder<JavaFieldAccess> result = ImmutableSet.builder();
        for (JavaField field : fields) {
            result.addAll(field.getAccessesToSelf());
        }
        return result.build();
    }

    public static DescribedPredicate<JavaClass> withType(final Class<?> type) {
        return DescribedPredicate.<Class<?>>equalTo(type).onResultOf(REFLECT).as("with type " + type.getName());
    }

    public static DescribedPredicate<Class<?>> reflectionAssignableTo(final Class<?> type) {
        checkNotNull(type);
        return new DescribedPredicate<Class<?>>("assignable to " + type.getName()) {
            @Override
            public boolean apply(Class<?> input) {
                return type.isAssignableFrom(input);
            }
        };
    }

    public static DescribedPredicate<Class<?>> reflectionAssignableFrom(final Class<?> type) {
        checkNotNull(type);
        return new DescribedPredicate<Class<?>>("assignable from " + type.getName()) {
            @Override
            public boolean apply(Class<?> input) {
                return input.isAssignableFrom(type);
            }
        };
    }

    public static DescribedPredicate<JavaClass> assignableTo(final Class<?> type) {
        return reflectionAssignableTo(type).onResultOf(REFLECT);
    }

    public static DescribedPredicate<JavaClass> assignableFrom(final Class<?> type) {
        return reflectionAssignableFrom(type).onResultOf(REFLECT);
    }

    public static final DescribedPredicate<JavaClass> INTERFACES = new DescribedPredicate<JavaClass>("interfaces") {
        @Override
        public boolean apply(JavaClass input) {
            return input.isInterface();
        }
    };

    public static final Function<JavaClass, Class<?>> REFLECT = new Function<JavaClass, Class<?>>() {
        @Override
        public Class<?> apply(JavaClass input) {
            return input.reflect();
        }
    };

    class CompletionProcess {
        AccessContext.Part completeCodeUnitsFrom(ImportContext context) {
            AccessContext.Part part = new AccessContext.Part();
            for (JavaCodeUnit codeUnit : codeUnits) {
                part.mergeWith(codeUnit.completeFrom(context));
            }
            return part;
        }
    }

    static final class Builder {
        private TypeDetails typeDetails;
        private final Set<BuilderWithBuildParameter<JavaClass, JavaField>> fieldBuilders = new HashSet<>();
        private final Set<BuilderWithBuildParameter<JavaClass, JavaMethod>> methodBuilders = new HashSet<>();
        private final Set<BuilderWithBuildParameter<JavaClass, JavaConstructor>> constructorBuilders = new HashSet<>();
        private Optional<JavaStaticInitializer.Builder> staticInitializerBuilder = Optional.absent();
        private final Set<JavaAnnotation.Builder> annotations = new HashSet<>();

        @SuppressWarnings("unchecked")
        Builder withType(TypeDetails typeDetails) {
            this.typeDetails = typeDetails;
            return this;
        }

        Builder addField(BuilderWithBuildParameter<JavaClass, JavaField> fieldBuilder) {
            fieldBuilders.add(fieldBuilder);
            return this;
        }

        Builder addMethod(BuilderWithBuildParameter<JavaClass, JavaMethod> methodBuilder) {
            methodBuilders.add(methodBuilder);
            return this;
        }

        Builder addConstructor(BuilderWithBuildParameter<JavaClass, JavaConstructor> constructorBuilder) {
            constructorBuilders.add(constructorBuilder);
            return this;
        }

        Builder withStaticInitializer(JavaStaticInitializer.Builder builder) {
            checkState(!staticInitializerBuilder.isPresent(),
                    "Tried to add a second static initializer to %s, this is most likely a bug",
                    typeDetails.getName());
            staticInitializerBuilder = Optional.of(builder);
            return this;
        }

        Builder withAnnotations(Set<JavaAnnotation.Builder> annotations) {
            this.annotations.addAll(annotations);
            return this;
        }

        public JavaClass build() {
            return new JavaClass(this);
        }
    }
}
