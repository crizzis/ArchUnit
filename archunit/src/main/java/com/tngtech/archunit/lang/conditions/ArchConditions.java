package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.AccessTarget;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.core.properties.HasOwner.Functions.Get;
import com.tngtech.archunit.core.properties.HasOwner.Predicates.With;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassGetsFieldCondition;
import com.tngtech.archunit.lang.conditions.ClassAccessesFieldCondition.ClassSetsFieldCondition;

import static com.tngtech.archunit.core.Formatters.ensureSimpleName;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.fieldAccessTarget;
import static com.tngtech.archunit.core.properties.HasName.Predicates.name;

public final class ArchConditions {
    private ArchConditions() {
    }

    /**
     * @param packageIdentifier A String identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching the identifier
     */
    public static ArchCondition<JavaClass> accessClassesThatResideIn(String packageIdentifier) {
        return accessClassesThatResideInAnyPackage(packageIdentifier).
                as("access classes that reside in package '%s'", packageIdentifier);
    }

    /**
     * @param packageIdentifiers Strings identifying a package according to {@link PackageMatcher}
     * @return A condition matching accesses to packages matching any of the identifiers
     */
    public static ArchCondition<JavaClass> accessClassesThatResideInAnyPackage(String... packageIdentifiers) {
        return new ClassAccessesAnyPackageCondition(packageIdentifiers);
    }

    /**
     * @param packageIdentifiers Strings identifying packages according to {@link PackageMatcher}
     * @return A condition matching accesses by packages matching any of the identifiers
     */
    public static ArchCondition<JavaClass> onlyBeAccessedByAnyPackage(String... packageIdentifiers) {
        return new ClassIsOnlyAccessedByAnyPackageCondition(packageIdentifiers);
    }

    public static ArchCondition<JavaClass> getField(final Class<?> owner, final String fieldName) {
        return getField(owner.getName(), fieldName);
    }

    public static ArchCondition<JavaClass> getField(final String ownerName, final String fieldName) {
        return getFieldWhere(ownerAndNameAre(ownerName, fieldName))
                .as("get field %s.%s", ensureSimpleName(ownerName), fieldName);
    }

    public static ArchCondition<JavaClass> getFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassGetsFieldCondition(predicate)
                .as("get field where " + predicate.getDescription());
    }

    public static ArchCondition<JavaClass> setField(final Class<?> owner, final String fieldName) {
        return setField(owner.getName(), fieldName);
    }

    public static ArchCondition<JavaClass> setField(final String ownerName, final String fieldName) {
        return setFieldWhere(ownerAndNameAre(ownerName, fieldName))
                .as("set field %s.%s", ensureSimpleName(ownerName), fieldName);
    }

    public static ArchCondition<JavaClass> setFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassSetsFieldCondition(predicate)
                .as("set field where " + predicate.getDescription());
    }

    public static ArchCondition<JavaClass> accessField(final Class<?> owner, final String fieldName) {
        return accessField(owner.getName(), fieldName);
    }

    public static ArchCondition<JavaClass> accessField(final String ownerName, final String fieldName) {
        return accessFieldWhere(ownerAndNameAre(ownerName, fieldName))
                .as("access field %s.%s", ensureSimpleName(ownerName), fieldName);
    }

    public static ArchCondition<JavaClass> accessFieldWhere(DescribedPredicate<? super JavaFieldAccess> predicate) {
        return new ClassAccessesFieldCondition(predicate)
                .as("access field where " + predicate.getDescription());
    }

    public static ArchCondition<JavaClass> callMethodWhere(final DescribedPredicate<? super JavaMethodCall> predicate) {
        return new ClassCallsCodeUnitCondition(new DescribedPredicate<JavaCall<?>>(predicate.getDescription()) {
            @Override
            public boolean apply(JavaCall<?> input) {
                return input instanceof JavaMethodCall && predicate.apply((JavaMethodCall) input);
            }
        });
    }

    public static ArchCondition<JavaClass> callCodeUnitWhere(DescribedPredicate<? super JavaCall<?>> predicate) {
        return new ClassCallsCodeUnitCondition(predicate);
    }

    public static ArchCondition<JavaClass> accessClass(final DescribedPredicate<? super JavaClass> predicate) {
        return new ClassAccessesCondition(predicate);
    }

    public static ArchCondition<JavaClass> resideInAPackage(String packageIdentifier) {
        return new ClassResidesInCondition(packageIdentifier);
    }

    public static <T> ArchCondition<T> never(ArchCondition<T> condition) {
        return new NeverCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containAnyElementThat(ArchCondition<T> condition) {
        return new ContainAnyCondition<>(condition);
    }

    public static <T> ArchCondition<Collection<? extends T>> containOnlyElementsThat(ArchCondition<T> condition) {
        return new ContainsOnlyCondition<>(condition);
    }

    private static DescribedPredicate<JavaFieldAccess> ownerAndNameAre(String ownerName, final String fieldName) {
        return fieldAccessTarget(With.<JavaClass>owner(name(ownerName)))
                .and(fieldAccessTarget(name(fieldName)))
                .as(ownerName + "." + fieldName);
    }

    private static class ClassAccessesCondition extends AnyAttributeMatchesCondition<JavaAccess<?>> {
        ClassAccessesCondition(final DescribedPredicate<? super JavaClass> predicate) {
            super(new JavaAccessCondition(accessWithOwnerWith(predicate)));
        }

        private static DescribedPredicate<JavaAccess<?>> accessWithOwnerWith(DescribedPredicate<? super JavaClass> predicate) {
            ChainableFunction<JavaAccess<?>, AccessTarget> getTarget = JavaAccess.Functions.Get.target();
            return predicate.onResultOf(Get.<JavaClass>owner().after(getTarget))
                    .as("access class " + predicate.getDescription());
        }

        @Override
        Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
            return item.getAccessesFromSelf();
        }
    }

    public static ArchCondition<JavaClass> beNamed(final String name) {
        return new ArchCondition<JavaClass>(String.format("be named '%s'", name)) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean matches = item.getName().equals(name);
                String infix = matches ? "" : "not ";
                String message = String.format("class %s is %snamed '%s'", item.getName(), infix, name);
                events.add(new SimpleConditionEvent<>(item, matches, message));
            }
        };
    }
}
