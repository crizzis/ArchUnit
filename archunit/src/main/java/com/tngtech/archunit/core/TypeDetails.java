package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.core.ReflectionUtils.classForName;

public class TypeDetails {
    private Optional<TypeDetails> enclosingClass = Optional.absent();
    private Optional<TypeDetails> superClass = Optional.absent();
    private List<TypeDetails> interfaces = Collections.emptyList();
    private String name;
    private String simpleName;
    private String javaPackage;
    private boolean isInterface;
    private final Class<?> type;

    @Deprecated // FIXME: Get rid of this constructor as soon as reflection is gone
    private TypeDetails(Class<?> type) {
        this.type = type;
        enclosingClass = type.getEnclosingClass() != null ?
                Optional.of(TypeDetails.of(type.getEnclosingClass())) :
                Optional.<TypeDetails>absent();
        superClass = type.getSuperclass() != null ?
                Optional.of(TypeDetails.of(type.getSuperclass())) :
                Optional.<TypeDetails>absent();
        interfaces = TypeDetails.allOf(type.getInterfaces());
        name = type.getName();
        simpleName = type.getSimpleName();
        javaPackage = type.getPackage() != null ? type.getPackage().getName() : "";
        isInterface = type.isInterface();
    }

    private TypeDetails(Type type) {
        this(type.getClassName());
    }

    private TypeDetails(String fullName) {
        this.type = null;
        this.name = fullName;
        this.simpleName = fullName.replaceAll("^.*(\\.|\\$)", "");
        this.javaPackage = fullName.replaceAll("(\\.|\\$).*$", "");
        isInterface = false;
    }

    Set<JavaAnnotation> getAnnotations() {
        return type != null ? JavaAnnotation.allOf(type.getAnnotations()) : Collections.<JavaAnnotation>emptySet();
    }

    public String getName() {
        return name;
    }

    Optional<TypeDetails> getEnclosingClass() {
        return enclosingClass;
    }

    List<TypeDetails> getInterfaces() {
        return interfaces;
    }

    Optional<TypeDetails> getSuperclass() {
        return superClass;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getPackage() {
        return javaPackage;
    }

    public boolean isInterface() {
        return isInterface;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TypeDetails other = (TypeDetails) obj;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name + "}";
    }

    public static List<TypeDetails> allOf(Class<?>... types) {
        return allOf(ImmutableList.copyOf(types));
    }

    public static List<TypeDetails> allOf(Collection<Class<?>> types) {
        ImmutableList.Builder<TypeDetails> result = ImmutableList.builder();
        for (Class<?> type : types) {
            result.add(TypeDetails.of(type));
        }
        return result.build();
    }

    public static List<TypeDetails> allOf(Type[] types) {
        ImmutableList.Builder<TypeDetails> result = ImmutableList.builder();
        for (Type type : types) {
            result.add(TypeDetails.of(type));
        }
        return result.build();
    }

    public static TypeDetails of(Class<?> type) {
        return new TypeDetails(type);
    }

    public static TypeDetails of(Type type) {
        try {
            return new TypeDetails(classForName(type.getClassName()));
        } catch (ReflectionException e) {
            return new TypeDetails(type);
        }
    }

    public static TypeDetails of(String typeName) {
        return new TypeDetails(typeName);
    }
}
