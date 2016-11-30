package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

public class JavaAnnotation {
    private final TypeDetails type;
    private final Map<String, Object> values;

    public JavaAnnotation(TypeDetails type, Map<String, Object> values) {
        this.type = checkNotNull(type);
        this.values = checkNotNull(values);
    }

    public TypeDetails getType() {
        return type;
    }

    /**
     * Returns the value of the property with the given name, i.e. the result of the method with the property name
     * of the represented {@link java.lang.annotation.Annotation}. E.g. for
     * <pre><code>      {@literal @}SomeAnnotation(value = "someString", types = {SomeType.class, AnotherType.class})
     * class SomeAnnotatedClass {}
     * </code></pre>
     * the results will be
     * <pre><code>       someAnnotation.get("value") -> "someString"
     * someAnnotation.get("types") -> [TypeDetails{SomeType}, TypeDetails{AnotherType}]
     * </code></pre>
     *
     * @param property The name of the annotation property, i.e. the declared method name
     * @return the value of the given property, where the result type is more precisely
     * <ul>
     * <li>Class&lt;?&gt; -> TypeDetails{clazz}</li>
     * <li>Class&lt;?&gt;[] -> [TypeDetails{clazz},...]</li>
     * <li>Enum -> JavaEnumConstant</li>
     * <li>Enum[] -> [JavaEnumConstant,...]</li>
     * <li>anyOtherType -> anyOtherType</li>
     * </ul>
     */
    public Object get(String property) {
        return values.get(property);
    }

    /**
     * @return a map containing all [property -> value], where each value is derived via {@link #get(String property)}
     */
    public Map<String, Object> getProperties() {
        return values;
    }

    public static Set<JavaAnnotation> of(Annotation[] reflectionAnnotations) {
        ImmutableSet.Builder<JavaAnnotation> result = ImmutableSet.builder();
        for (Annotation annotation : reflectionAnnotations) {
            result.add(new JavaAnnotation(TypeDetails.of(annotation.annotationType()), mapOf(annotation)));
        }
        return result.build();
    }

    private static Map<String, Object> mapOf(Annotation annotation) {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            result.put(method.getName(), get(annotation, method.getName()));
        }
        return result.build();
    }

    private static Object get(Annotation annotation, String methodName) {
        try {
            Object result = annotation.annotationType().getMethod(methodName).invoke(annotation);
            if (result instanceof Class) {
                return TypeDetails.of((Class<?>) result);
            }
            if (result instanceof Class[]) {
                List<TypeDetails> typeDetails = TypeDetails.allOf((Class<?>[]) result);
                return typeDetails.toArray(new TypeDetails[typeDetails.size()]);
            }
            if (result instanceof Enum<?>) {
                return enumConstant((Enum) result);
            }
            if (result instanceof Enum[]) {
                return enumConstants((Enum[]) result);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static JavaEnumConstant[] enumConstants(Enum[] enums) {
        List<JavaEnumConstant> result = new ArrayList<>();
        for (Enum e : enums) {
            result.add(enumConstant(e));
        }
        return result.toArray(new JavaEnumConstant[result.size()]);
    }

    private static JavaEnumConstant enumConstant(Enum result) {
        return new JavaEnumConstant(TypeDetails.of(result.getDeclaringClass()), result.name());
    }
}
