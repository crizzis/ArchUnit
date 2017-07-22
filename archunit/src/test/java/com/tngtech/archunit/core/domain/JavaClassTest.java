package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.testobjects.ADependingOnB;
import com.tngtech.archunit.core.domain.testobjects.B;
import com.tngtech.archunit.core.domain.testobjects.InterfaceForA;
import com.tngtech.archunit.core.domain.testobjects.SuperA;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.Condition;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Assert;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableFrom;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.domain.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Conditions.codeUnitWithSignature;
import static com.tngtech.archunit.testutil.Conditions.containing;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaClassTest {

    @Test
    public void finds_fields_and_methods() {
        JavaClass javaClass = javaClassViaReflection(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(javaClass.reflect()).isEqualTo(ClassWithTwoFieldsAndTwoMethods.class);
        assertThat(javaClass.getFields()).hasSize(2);
        assertThat(javaClass.getMethods()).hasSize(2);

        for (JavaField field : javaClass.getFields()) {
            assertThat(field.getOwner()).isSameAs(javaClass);
        }
        for (JavaCodeUnit method : javaClass.getCodeUnits()) {
            assertThat(method.getOwner()).isSameAs(javaClass);
        }
    }

    @Test
    public void finds_constructors() {
        JavaClass javaClass = javaClassViaReflection(ClassWithSeveralConstructors.class);

        assertThat(javaClass.getConstructors()).hasSize(3);
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, String.class)));
        assertThat(javaClass.getConstructors()).is(containing(codeUnitWithSignature(CONSTRUCTOR_NAME, int.class, Object[].class)));
    }

    @Test
    public void anonymous_class_has_package_of_declaring_class() {
        JavaClass anonymous = javaClassViaReflection(new Serializable() {}.getClass());

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void inner_class_has_package_of_declaring_class() {
        JavaClass anonymous = javaClassViaReflection(ClassWithInnerClass.Inner.class);

        assertThat(anonymous.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void Array_class_has_default_package() {
        JavaClass arrayType = javaClassViaReflection(JavaClassTest[].class);

        assertThat(arrayType.getPackage()).isEmpty();
    }

    @Test
    public void superclasses_are_found() {
        JavaClass clazz = javaClassesViaReflection(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getAllSuperClasses()).extracting("name").containsExactly(
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void hierarchy_is_found() {
        JavaClass clazz = javaClassesViaReflection(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);

        assertThat(clazz.getClassHierarchy()).extracting("name").containsExactly(
                clazz.getName(),
                SuperClassWithFieldAndMethod.class.getName(),
                Parent.class.getName(),
                Object.class.getName());
    }

    @Test
    public void all_classes_self_is_assignable_to() {
        JavaClass clazz = importClasses(
                ChildWithFieldAndMethod.class,
                ParentWithFieldAndMethod.class,
                InterfaceWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertThat(clazz.getAllClassesSelfIsAssignableTo())
                .extracting("name")
                .containsOnly(
                        ChildWithFieldAndMethod.class.getName(),
                        ParentWithFieldAndMethod.class.getName(),
                        InterfaceWithFieldAndMethod.class.getName(),
                        Object.class.getName());
    }

    @Test
    public void isAnnotatedWith_type() {
        assertThat(javaClassViaReflection(Parent.class).isAnnotatedWith(SomeAnnotation.class))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(javaClassViaReflection(Parent.class).isAnnotatedWith(Retention.class))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void isAnnotatedWith_typeName() {
        assertThat(javaClassViaReflection(Parent.class).isAnnotatedWith(SomeAnnotation.class.getName()))
                .as("Parent is annotated with @" + SomeAnnotation.class.getSimpleName()).isTrue();
        assertThat(javaClassViaReflection(Parent.class).isAnnotatedWith(Retention.class.getName()))
                .as("Parent is annotated with @" + Retention.class.getSimpleName()).isFalse();
    }

    @Test
    public void predicate_isAnnotatedWith() {
        assertThat(javaClassViaReflection(Parent.class)
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysTrue()))
                .as("predicate matches").isTrue();
        assertThat(javaClassViaReflection(Parent.class)
                .isAnnotatedWith(DescribedPredicate.<JavaAnnotation>alwaysFalse()))
                .as("predicate matches").isFalse();
    }

    @Test
    public void allAccesses_contains_accesses_from_superclass() {
        JavaClass javaClass = javaClassesViaReflection(ClassWithTwoFieldsAndTwoMethods.class, SuperClassWithFieldAndMethod.class, Parent.class)
                .get(ClassWithTwoFieldsAndTwoMethods.class);
        JavaClass anotherClass = javaClassViaReflection(Object.class);
        simulateCall().from(javaClass.getMethod("stringMethod"), 8).to(anotherClass.getMethod("toString"));
        simulateCall().from(javaClass.getSuperClass().get().getMethod("objectMethod"), 8).to(anotherClass.getMethod("toString"));

        assertThat(javaClass.getAccessesFromSelf()).extractingResultOf("getOriginOwner").containsOnly(javaClass);
        assertThat(javaClass.getAllAccessesFromSelf()).extractingResultOf("getOriginOwner")
                .containsOnly(javaClass, javaClass.getSuperClass().get());
    }

    @Test
    public void JavaClass_is_equivalent_to_reflect_type() {
        JavaClass list = javaClassViaReflection(List.class);

        assertThat(list.isEquivalentTo(List.class)).as("JavaClass is List.class").isTrue();
        assertThat(list.isEquivalentTo(Collection.class)).as("JavaClass is Collection.class").isFalse();
    }

    @Test
    public void getMembers_and_getAllMembers() {
        JavaClass clazz = importClasses(
                ChildWithFieldAndMethod.class,
                ParentWithFieldAndMethod.class,
                InterfaceWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertThat(clazz.getMembers())
                .extracting(memberIdentifier())
                .containsOnlyElementsOf(ChildWithFieldAndMethod.Members.MEMBERS);

        assertThat(clazz.getAllMembers())
                .filteredOn(isNotObject())
                .extracting(memberIdentifier())
                .containsOnlyElementsOf(ImmutableSet.<String>builder()
                        .addAll(ChildWithFieldAndMethod.Members.MEMBERS)
                        .addAll(ParentWithFieldAndMethod.Members.MEMBERS)
                        .addAll(InterfaceWithFieldAndMethod.Members.MEMBERS)
                        .build());
    }

    @Test
    public void getCodeUnitWithName() {
        final JavaClass clazz = importClasses(ChildWithFieldAndMethod.class).get(ChildWithFieldAndMethod.class);

        assertIllegalArgumentException("childMethod", new Runnable() {
            @Override
            public void run() {
                clazz.getCodeUnitWithParameterTypes("childMethod");
            }
        });
        assertIllegalArgumentException("childMethod", new Runnable() {
            @Override
            public void run() {
                clazz.getCodeUnitWithParameterTypes("childMethod", Object.class);
            }
        });
        assertIllegalArgumentException("wrong", new Runnable() {
            @Override
            public void run() {
                clazz.getCodeUnitWithParameterTypes("wrong", String.class);
            }
        });

        assertThat(clazz.getCodeUnitWithParameterTypes("childMethod", String.class))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, "childMethod", String.class));
        assertThat(clazz.getCodeUnitWithParameterTypeNames("childMethod", String.class.getName()))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, "childMethod", String.class));
        assertThat(clazz.getCodeUnitWithParameterTypes(CONSTRUCTOR_NAME, Object.class))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, CONSTRUCTOR_NAME, Object.class));
        assertThat(clazz.getCodeUnitWithParameterTypeNames(CONSTRUCTOR_NAME, Object.class.getName()))
                .is(equivalentCodeUnit(ChildWithFieldAndMethod.class, CONSTRUCTOR_NAME, Object.class));
    }

    private Condition<JavaCodeUnit> equivalentCodeUnit(final Class<?> owner, final String methodName, final Class<?> paramType) {
        return new Condition<JavaCodeUnit>() {
            @Override
            public boolean matches(JavaCodeUnit value) {
                return value.getOwner().isEquivalentTo(owner) &&
                        value.getName().equals(methodName) &&
                        value.getParameters().getNames().equals(ImmutableList.of(paramType.getName()));
            }
        };
    }

    @Test
    public void direct_dependencies_from_self() {
        JavaClass javaClass = importClasses(ADependingOnB.class, B.class).get(ADependingOnB.class);

        assertThat(javaClass.getDirectDependenciesFromSelf())
                .hasSize(6)
                .areAtLeastOne(extendsDependency()
                        .from(ADependingOnB.class)
                        .to(SuperA.class)
                        .inLineNumber(0))
                .areAtLeastOne(implementsDependency()
                        .from(ADependingOnB.class)
                        .to(InterfaceForA.class)
                        .inLineNumber(0))
                .areAtLeastOne(callDependency()
                        .from(ADependingOnB.class)
                        .to(SuperA.class, CONSTRUCTOR_NAME)
                        .inLineNumber(4))
                .areAtLeastOne(callDependency()
                        .from(ADependingOnB.class)
                        .to(B.class, CONSTRUCTOR_NAME)
                        .inLineNumber(5))
                .areAtLeastOne(setFieldDependency()
                        .from(ADependingOnB.class)
                        .to(B.class, "field")
                        .inLineNumber(6))
                .areAtLeastOne(callDependency()
                        .from(ADependingOnB.class)
                        .to(B.class, "call")
                        .inLineNumber(7));
    }

    @Test
    public void function_simpleName() {
        assertThat(JavaClass.Functions.SIMPLE_NAME.apply(javaClassViaReflection(List.class)))
                .as("result of SIMPLE_NAME(clazz)")
                .isEqualTo(List.class.getSimpleName());
    }

    @Test
    public void predicate_withType() {
        assertThat(type(Parent.class).apply(javaClassViaReflection(Parent.class)))
                .as("type(Parent) matches JavaClass Parent").isTrue();
        assertThat(type(Parent.class).apply(javaClassViaReflection(SuperClassWithFieldAndMethod.class)))
                .as("type(Parent) matches JavaClass SuperClassWithFieldAndMethod").isFalse();

        assertThat(type(System.class).getDescription()).isEqualTo("type java.lang.System");
    }

    @Test
    public void predicate_simpleName() {
        assertThat(simpleName(Parent.class.getSimpleName()).apply(javaClassViaReflection(Parent.class)))
                .as("simpleName(Parent) matches JavaClass Parent").isTrue();
        assertThat(simpleName(Parent.class.getSimpleName()).apply(javaClassViaReflection(SuperClassWithFieldAndMethod.class)))
                .as("simpleName(Parent) matches JavaClass SuperClassWithFieldAndMethod").isFalse();

        assertThat(simpleName("Simple").getDescription()).isEqualTo("simple name 'Simple'");
    }

    @Test
    public void predicate_assignableFrom() {
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(ClassWithTwoFieldsAndTwoMethods.class)
                .via(SuperClassWithFieldAndMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(InterfaceWithMethod.class)
                .to(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().from(Parent.class)
                .to(InterfaceWithMethod.class)
                .isFalse();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(Parent.class)
                .isTrue();
        assertThatAssignable().from(SuperClassWithFieldAndMethod.class)
                .to(ClassWithTwoFieldsAndTwoMethods.class)
                .isFalse();
        assertThatAssignable().from(Parent.class)
                .to(SuperClassWithFieldAndMethod.class)
                .isFalse();

        assertThat(assignableFrom(System.class).getDescription()).isEqualTo("assignable from java.lang.System");
    }

    @Test
    public void predicate_assignableTo() {
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(ClassWithTwoFieldsAndTwoMethods.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isFalse();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(InterfaceWithMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .via(SuperClassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(InterfaceWithMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(Parent.class)
                .isFalse();
        assertThatAssignable().to(SuperClassWithFieldAndMethod.class)
                .from(ClassWithTwoFieldsAndTwoMethods.class)
                .isTrue();
        assertThatAssignable().to(Parent.class)
                .from(SuperClassWithFieldAndMethod.class)
                .isTrue();

        assertThat(assignableTo(System.class).getDescription()).isEqualTo("assignable to java.lang.System");
    }

    @Test
    public void predicate_interfaces() {
        assertThat(INTERFACES.apply(javaClassViaReflection(Serializable.class))).as("Predicate matches").isTrue();
        assertThat(INTERFACES.apply(javaClassViaReflection(Object.class))).as("Predicate matches").isFalse();
        assertThat(INTERFACES.getDescription()).isEqualTo("interfaces");
    }

    @Test
    public void predicate_reside_in_a_package() {
        JavaClass clazz = fakeClassWithPackage("some.arbitrary.pkg");

        assertThat(resideInAPackage("some..pkg").apply(clazz)).as("package matches").isTrue();

        clazz = fakeClassWithPackage("wrong.arbitrary.pkg");

        assertThat(resideInAPackage("some..pkg").apply(clazz)).as("package matches").isFalse();

        assertThat(resideInAPackage("..any..").getDescription())
                .isEqualTo("reside in a package '..any..'");
    }

    @Test
    public void predicate_reside_in_any_package() {
        JavaClass clazz = fakeClassWithPackage("some.arbitrary.pkg");

        assertThat(resideInAnyPackage("any.thing", "some..pkg").apply(clazz)).as("package matches").isTrue();

        clazz = fakeClassWithPackage("wrong.arbitrary.pkg");

        assertThat(resideInAnyPackage("any.thing", "some..pkg").apply(clazz)).as("package matches").isFalse();

        assertThat(resideInAnyPackage("any.thing", "..any..").getDescription())
                .isEqualTo("reside in any package ['any.thing', '..any..']");
    }

    @Test
    public void predicate_equivalentTo() {
        JavaClass javaClass = importClasses(SuperClassWithFieldAndMethod.class, Parent.class).get(SuperClassWithFieldAndMethod.class);

        assertThat(equivalentTo(SuperClassWithFieldAndMethod.class).apply(javaClass))
                .as("predicate matches").isTrue();
        assertThat(equivalentTo(Parent.class).apply(javaClass))
                .as("predicate matches").isFalse();
        assertThat(equivalentTo(Parent.class).getDescription())
                .as("description").isEqualTo("equivalent to " + Parent.class.getName());
    }

    private static DependencyConditionCreation callDependency() {
        return new DependencyConditionCreation("calls");
    }

    private static DependencyConditionCreation setFieldDependency() {
        return new DependencyConditionCreation("sets");
    }

    private static DependencyConditionCreation implementsDependency() {
        return new DependencyConditionCreation("implements");
    }

    private static DependencyConditionCreation extendsDependency() {
        return new DependencyConditionCreation("extends");
    }

    private static class DependencyConditionCreation {
        private final String descriptionPart;

        DependencyConditionCreation(String descriptionPart) {
            this.descriptionPart = descriptionPart;
        }

        Step2 from(Class<?> origin) {
            return new Step2(origin);
        }

        private class Step2 {
            private final Class<?> origin;

            Step2(Class<?> origin) {
                this.origin = origin;
            }

            Step3 to(Class<?> target) {
                return new Step3(target);
            }

            Step3 to(Class<?> target, String targetName) {
                return new Step3(target, targetName);
            }

            private class Step3 {
                private final Class<?> target;
                private final String targetDescription;

                Step3(Class<?> target) {
                    this.target = target;
                    targetDescription = target.getSimpleName();
                }

                Step3(Class<?> target, String targetName) {
                    this.target = target;
                    targetDescription = target.getSimpleName() + "." + targetName;
                }

                Condition<Dependency> inLineNumber(final int lineNumber) {
                    return new Condition<Dependency>(String.format(
                            "%s %s %s in line %d", origin.getName(), descriptionPart, targetDescription, lineNumber)) {
                        @Override
                        public boolean matches(Dependency value) {
                            return value.getOriginClass().isEquivalentTo(origin) &&
                                    value.getTargetClass().isEquivalentTo(target) &&
                                    value.getDescription().matches(String.format(".*%s.*%s.*%s.*:%d.*",
                                            origin.getSimpleName(), descriptionPart, targetDescription, lineNumber));
                        }
                    };
                }
            }
        }
    }

    private void assertIllegalArgumentException(String expectedMessagePart, Runnable runnable) {
        try {
            runnable.run();
            Assert.fail("Should have thrown an " + IllegalArgumentException.class.getSimpleName());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .as("Messagee of %s", IllegalArgumentException.class.getSimpleName())
                    .contains(expectedMessagePart);
        }
    }

    private Extractor<JavaMember, String> memberIdentifier() {
        return new Extractor<JavaMember, String>() {
            @Override
            public String extract(JavaMember input) {
                return input.getOwner().getSimpleName() + "#" + input.getName();
            }
        };
    }

    private Condition<JavaMember> isNotObject() {
        return new Condition<JavaMember>() {
            @Override
            public boolean matches(JavaMember value) {
                return !value.getOwner().isEquivalentTo(Object.class);
            }
        };
    }

    private static JavaClass fakeClassWithPackage(String pkg) {
        JavaClass javaClass = mock(JavaClass.class);
        when(javaClass.getPackage()).thenReturn(pkg);
        return javaClass;
    }

    private static AssignableAssert assertThatAssignable() {
        return new AssignableAssert();
    }

    private static class AssignableAssert {
        private String message;
        private Set<DescribedPredicate<JavaClass>> assignable = new HashSet<>();
        private Class<?> firstType;

        public FromEvaluation from(final Class<?> type) {
            firstType = type;
            message = String.format("assignableFrom(%s) matches ", type.getSimpleName());
            assignable = ImmutableSet.of(new DescribedPredicate<JavaClass>("direct assignable from") {
                @Override
                public boolean apply(JavaClass input) {
                    return input.isAssignableFrom(type) && input.isAssignableFrom(type.getName());
                }
            }, assignableFrom(type), assignableFrom(type.getName()));
            return new FromEvaluation();
        }

        public ToEvaluation to(final Class<?> type) {
            firstType = type;
            message = String.format("assignableTo(%s) matches ", type.getSimpleName());
            assignable = ImmutableSet.of(new DescribedPredicate<JavaClass>("direct assignable to") {
                @Override
                public boolean apply(JavaClass input) {
                    return input.isAssignableTo(type) && input.isAssignableTo(type.getName());
                }
            }, assignableTo(type), assignableTo(type.getName()));
            return new ToEvaluation();
        }

        private class FromEvaluation extends Evaluation<FromEvaluation> {
            public FromEvaluation to(Class<?> toType) {
                return evaluationToType(toType);
            }
        }

        private class ToEvaluation extends Evaluation<ToEvaluation> {
            public ToEvaluation from(Class<?> fromType) {
                return evaluationToType(fromType);
            }
        }

        private class Evaluation<SELF> {
            private List<AbstractBooleanAssert<?>> assignableAssertion = new ArrayList<>();

            private final Set<Class<?>> additionalTypes = new HashSet<>();

            // NOTE: We need all the classes in the context to create realistic hierarchies
            SELF via(Class<?> type) {
                additionalTypes.add(type);
                return self();
            }

            @SuppressWarnings("unchecked")
            private SELF self() {
                return (SELF) this;
            }

            SELF evaluationToType(Class<?> secondType) {
                Class<?>[] types = ImmutableSet.<Class<?>>builder()
                        .addAll(additionalTypes).add(firstType).add(secondType)
                        .build().toArray(new Class<?>[0]);
                JavaClass javaClass = javaClassesViaReflection(types).get(secondType);
                for (DescribedPredicate<JavaClass> predicate : assignable) {
                    assignableAssertion.add(assertThat(predicate.apply(javaClass))
                            .as(message + secondType.getSimpleName()));
                }
                return self();
            }

            public void isTrue() {
                for (AbstractBooleanAssert<?> assertion : assignableAssertion) {
                    assertion.isTrue();
                }
            }

            public void isFalse() {
                for (AbstractBooleanAssert<?> assertion : assignableAssertion) {
                    assertion.isFalse();
                }
            }
        }
    }

    static class ClassWithTwoFieldsAndTwoMethods extends SuperClassWithFieldAndMethod {
        String stringField;
        private int intField;

        void voidMethod() {
        }

        protected String stringMethod() {
            return null;
        }
    }

    abstract static class SuperClassWithFieldAndMethod extends Parent implements InterfaceWithMethod {
        private Object objectField;

        @Override
        public Object objectMethod() {
            return null;
        }
    }

    interface InterfaceWithMethod {
        Object objectMethod();
    }

    @SomeAnnotation
    abstract static class Parent {
    }

    static class ClassWithSeveralConstructors {
        private ClassWithSeveralConstructors() {
        }

        ClassWithSeveralConstructors(String string) {
        }

        public ClassWithSeveralConstructors(int number, Object[] objects) {
        }
    }

    static class ClassWithInnerClass {
        class Inner {
        }
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    private static class ParentWithFieldAndMethod implements InterfaceWithFieldAndMethod {
        static class Members {
            // If we put this in the class, we affect tests for members
            static final Set<String> MEMBERS = ImmutableSet.of(
                    "ParentWithFieldAndMethod#parentField",
                    "ParentWithFieldAndMethod#parentMethod",
                    "ParentWithFieldAndMethod#" + CONSTRUCTOR_NAME);
        }

        Object parentField;

        ParentWithFieldAndMethod(Object parentField) {
            this.parentField = parentField;
        }

        @Override
        public void parentMethod() {
        }
    }

    private static class ChildWithFieldAndMethod extends ParentWithFieldAndMethod {
        static class Members {
            // If we put this in the class, we affect tests for members
            static final Set<String> MEMBERS = ImmutableSet.of(
                    "ChildWithFieldAndMethod#childField",
                    "ChildWithFieldAndMethod#childMethod",
                    "ChildWithFieldAndMethod#" + CONSTRUCTOR_NAME);
        }

        Object childField;

        ChildWithFieldAndMethod(Object childField) {
            super(childField);
            this.childField = childField;
        }

        void childMethod(String param) {
        }
    }

    private interface InterfaceWithFieldAndMethod {
        class Members {
            // If we put this in the class, we affect tests for members
            static final Set<String> MEMBERS = ImmutableSet.of(
                    "InterfaceWithFieldAndMethod#interfaceField",
                    "InterfaceWithFieldAndMethod#parentMethod");
        }

        String interfaceField = "foo";

        void parentMethod();
    }

}