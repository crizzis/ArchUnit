/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.Node;

import static com.tngtech.archunit.junit.ArchTestInitializationException.WRAP_CAUSE;
import static com.tngtech.archunit.junit.ReflectionUtils.getValueOrThrowException;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public abstract class AbstractArchUnitTestDescriptor<
        EXECUTION_CONTEXT extends EngineExecutionContext,
        CHILD_CONTEXT_SUPERTYPE extends TestDescriptor & Node<EXECUTION_CONTEXT> & CreatesChildren>
        extends AbstractTestDescriptor
        implements Node<EXECUTION_CONTEXT>, CreatesChildren {

    protected final TestDescriptorFactory<EXECUTION_CONTEXT, CHILD_CONTEXT_SUPERTYPE> factory;
    protected final AnnotationConfigFactory annotationConfigFactory;
    private final AnnotatedElementComposite annotatedElement;

    public AbstractArchUnitTestDescriptor(
            TestDescriptorFactory<EXECUTION_CONTEXT, CHILD_CONTEXT_SUPERTYPE> factory,
            AnnotationConfigFactory annotationConfigFactory,
            UniqueId uniqueId,
            String displayName,
            TestSource source,
            AnnotatedElement... annotatedElements) {
        super(uniqueId, displayName, source);
        this.factory = factory;
        this.annotationConfigFactory = annotationConfigFactory;
        this.annotatedElement = AnnotatedElementComposite.of(annotatedElements);
    }

    private boolean shouldBeUnconditionallyIgnored() {
        return AnnotationUtils.streamAnnotations(annotatedElement, annotationConfigFactory.getIgnoredAnnotations())
                .findFirst()
                .isPresent();
    }

    private Set<TestTag> findTagsOn(AnnotatedElementComposite annotatedElement) {
        return AnnotationUtils.streamRepeatableAnnotations(annotatedElement, annotationConfigFactory.getTagAnnotations())
                .map(annotation -> TestTag.create(ReflectionUtils.invokeMethod(annotation, "value")))
                .collect(toSet());
    }

    @Override
    public SkipResult shouldBeSkipped(EXECUTION_CONTEXT context) {
        if (shouldBeUnconditionallyIgnored()) {
            return SkipResult.skip("Ignored using @Disabled / @ArchIgnore");
        }
        return SkipResult.doNotSkip();
    }

    @Override
    public Set<TestTag> getTags() {
        Set<TestTag> result = findTagsOn(annotatedElement);
        result.addAll(getParent().map(TestDescriptor::getTags).orElse(emptySet()));
        return result;
    }

    public AnnotatedElementComposite getAnnotatedElement() {
        return annotatedElement;
    }

    protected void resolveChildren(
            TestDescriptor parent, ElementResolver resolver, Field field, Supplier<JavaClasses> classes) {

        if (ArchTests.class.isAssignableFrom(field.getType())) {
            resolveArchRules(parent, resolver, field, classes);
        } else {
            parent.addChild(factory.forTestField(resolver.getUniqueId(), getValue(field), classes, field));
        }
    }

    private void resolveArchRules(
            TestDescriptor parent, ElementResolver resolver, Field field, Supplier<JavaClasses> classes) {

        ArchUnitTestDescriptor.DeclaredArchTests archTests = getDeclaredArchTests(field);

        resolver.resolveClass(archTests.getDefinitionLocation())
                .ifRequestedAndResolved(CreatesChildren::createChildren)
                .ifRequestedButUnresolved((clazz, childResolver) -> {
                    CHILD_CONTEXT_SUPERTYPE rulesDescriptor = factory.forTestSuite(childResolver, archTests, classes, field);
                    parent.addChild(rulesDescriptor);
                    rulesDescriptor.createChildren(childResolver);
                });
    }

    private static ArchUnitTestDescriptor.DeclaredArchTests getDeclaredArchTests(Field field) {
        return new ArchUnitTestDescriptor.DeclaredArchTests(getValue(field));
    }

    private static <T> T getValue(Field field) {
        return getValueOrThrowException(field, field.getDeclaringClass(), WRAP_CAUSE);
    }
}
