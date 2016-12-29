package com.tngtech.archunit.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

class ClassFileImportRecord {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileImportRecord.class);

    private final Map<String, JavaClass> classes = new HashMap<>();

    private final Map<String, String> superClassNamesByOwner = new HashMap<>();
    private final SetMultimap<String, String> interfaceNamesByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaField.Builder> fieldBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaMethod.Builder> methodBuildersByOwner = HashMultimap.create();
    private final SetMultimap<String, JavaConstructor.Builder> constructorBuildersByOwner = HashMultimap.create();
    private final Map<String, JavaStaticInitializer.Builder> staticInitializerBuildersByOwner = new HashMap<>();
    private final SetMultimap<String, JavaAnnotation.Builder> annotationsByOwner = HashMultimap.create();
    private final EnclosingClassesByInnerClasses enclosingClassNamesByOwner = new EnclosingClassesByInnerClasses();

    private final Set<RawAccessRecord.ForField> rawFieldAccessRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawMethodCallRecords = new HashSet<>();
    private final Set<RawAccessRecord> rawConstructorCallRecords = new HashSet<>();

    void setSuperClass(String ownerName, String superClassName) {
        checkState(!superClassNamesByOwner.containsKey(ownerName),
                "Attempted to add %s as a second superclass to %s, this is most likely a bug",
                superClassName, ownerName);
        superClassNamesByOwner.put(ownerName, superClassName);
    }

    void addInterfaces(String ownerName, Set<String> interfaceNames) {
        interfaceNamesByOwner.putAll(ownerName, interfaceNames);
    }

    void addField(String ownerName, JavaField.Builder fieldBuilder) {
        fieldBuildersByOwner.put(ownerName, fieldBuilder);
    }

    void addMethod(String ownerName, JavaMethod.Builder methodBuilder) {
        methodBuildersByOwner.put(ownerName, methodBuilder);
    }

    void addConstructor(String ownerName, JavaConstructor.Builder constructorBuilder) {
        constructorBuildersByOwner.put(ownerName, constructorBuilder);
    }

    void setStaticInitializer(String ownerName, JavaStaticInitializer.Builder builder) {
        checkState(!staticInitializerBuildersByOwner.containsKey(ownerName),
                "Tried to add a second static initializer to %s, this is most likely a bug",
                ownerName);
        staticInitializerBuildersByOwner.put(ownerName, builder);
    }

    void addAnnotations(String ownerName, Set<JavaAnnotation.Builder> annotations) {
        this.annotationsByOwner.putAll(ownerName, annotations);
    }

    void setEnclosingClass(String ownerName, String enclosingClassName) {
        enclosingClassNamesByOwner.register(ownerName, enclosingClassName);
    }

    Optional<String> getSuperClassFor(String name) {
        return Optional.fromNullable(superClassNamesByOwner.get(name));
    }

    Set<String> getInterfaceNamesFor(String ownerName) {
        return interfaceNamesByOwner.get(ownerName);
    }

    Set<JavaField.Builder> getFieldBuildersFor(String ownerName) {
        return fieldBuildersByOwner.get(ownerName);
    }

    Set<JavaMethod.Builder> getMethodBuildersFor(String ownerName) {
        return methodBuildersByOwner.get(ownerName);
    }

    Set<JavaConstructor.Builder> getConstructorBuildersFor(String ownerName) {
        return constructorBuildersByOwner.get(ownerName);
    }

    Optional<JavaStaticInitializer.Builder> getStaticInitializerBuilderFor(String ownerName) {
        return Optional.fromNullable(staticInitializerBuildersByOwner.get(ownerName));
    }

    Set<JavaAnnotation.Builder> getAnnotationsFor(String ownerName) {
        return annotationsByOwner.get(ownerName);
    }

    Optional<String> getEnclosingClassFor(String ownerName) {
        return enclosingClassNamesByOwner.get(ownerName);
    }

    void registerFieldAccess(RawAccessRecord.ForField record) {
        rawFieldAccessRecords.add(record);
    }

    void registerMethodCall(RawAccessRecord record) {
        rawMethodCallRecords.add(record);
    }

    void registerConstructorCall(RawAccessRecord record) {
        rawConstructorCallRecords.add(record);
    }

    Set<RawAccessRecord.ForField> getRawFieldAccessRecords() {
        return ImmutableSet.copyOf(rawFieldAccessRecords);
    }

    Set<RawAccessRecord> getRawMethodCallRecords() {
        return ImmutableSet.copyOf(rawMethodCallRecords);
    }

    Set<RawAccessRecord> getRawConstructorCallRecords() {
        return ImmutableSet.copyOf(rawConstructorCallRecords);
    }

    void addAll(Collection<JavaClass> javaClasses) {
        for (JavaClass javaClass : javaClasses) {
            classes.put(javaClass.getName(), javaClass);
        }
    }

    Map<String, JavaClass> getClasses() {
        return classes;
    }

    Set<RawAccessRecord> getAccessRecords() {
        return ImmutableSet.<RawAccessRecord>builder()
                .addAll(rawFieldAccessRecords)
                .addAll(rawMethodCallRecords)
                .addAll(rawConstructorCallRecords)
                .build();
    }

    Map<String, String> getSuperClassNamesBySubClass() {
        return superClassNamesByOwner;
    }

    SetMultimap<String, String> getInterfaceNamesBySubInterface() {
        return interfaceNamesByOwner;
    }

    // NOTE: ASM calls visitInnerClass and visitOuterClass several times, sometimes when the outer class is imported
    //       and sometimes again when the inner class is imported. To make it easier, we'll just deal with duplicate
    //       registrations, as there is no harm, as long as no conflicting information is recorded.
    private static class EnclosingClassesByInnerClasses {
        private final Map<String, String> innerToOuter = new HashMap<>();

        void register(String innerName, String outerName) {
            if (registeringAllowed(innerName, outerName)) {
                innerToOuter.put(innerName, outerName);
            }
        }

        private boolean registeringAllowed(String innerName, String outerName) {
            boolean registeringAllowed = !innerToOuter.containsKey(innerName) ||
                    innerToOuter.get(innerName).equals(outerName);

            if (!registeringAllowed) {
                LOG.warn("Skipping registering outer class {} for inner class {}, since already outer class {} was registered",
                        outerName, innerName, innerToOuter.get(innerName));
            }

            return registeringAllowed;
        }

        public Optional<String> get(String ownerName) {
            return Optional.fromNullable(innerToOuter.get(ownerName));
        }
    }
}
