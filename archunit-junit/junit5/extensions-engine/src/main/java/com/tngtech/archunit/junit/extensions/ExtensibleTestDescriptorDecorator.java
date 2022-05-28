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
package com.tngtech.archunit.junit.extensions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tngtech.archunit.junit.AbstractArchUnitTestDescriptor;
import com.tngtech.archunit.junit.AnnotatedElementComposite;
import com.tngtech.archunit.junit.AnnotationUtils;
import com.tngtech.archunit.junit.CreatesChildren;
import com.tngtech.archunit.junit.ElementResolver;
import com.tngtech.archunit.junit.ReflectionUtils;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.Node;

class ExtensibleTestDescriptorDecorator<
        CHILD_DESCRIPTOR_SUPERTYPE extends TestDescriptor & Node<ExtensibleArchUnitEngineExecutionContext> & CreatesChildren>
        implements TestDescriptor, Node<ExtensibleArchUnitEngineExecutionContext>, CreatesChildren {

    private final ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
    private final Map<NamespacedStore.NamespacedKey, Object> store = new ConcurrentHashMap<>();
    private final AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, CHILD_DESCRIPTOR_SUPERTYPE> decorated;

    ExtensibleTestDescriptorDecorator(final AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, CHILD_DESCRIPTOR_SUPERTYPE> decorated) {
        this.decorated = decorated;
    }

    Map<NamespacedStore.NamespacedKey, Object> getStore() {
        return store;
    }

    AnnotatedElementComposite getAnnotatedElement() {
        return decorated.getAnnotatedElement();
    }

    @Override
    public ExtensibleArchUnitEngineExecutionContext prepare(ExtensibleArchUnitEngineExecutionContext context) throws Exception {
        getExtensionsFromTestSource().forEach(context::registerExtension);
        return context;
    }

    @Override
    public SkipResult shouldBeSkipped(ExtensibleArchUnitEngineExecutionContext context) {
        SkipResult skipResult = decorated.shouldBeSkipped(context);
        if (!skipResult.isSkipped()) {
            return toSkipResult(conditionEvaluator.evaluate(
                    context,
                    new ArchUnitExtensionContext(this, context))
            );
        }
        return skipResult;
    }

    private SkipResult toSkipResult(ConditionEvaluationResult evaluationResult) {
        if (evaluationResult.isDisabled()) {
            return SkipResult.skip(evaluationResult.getReason().orElse("<unknown>"));
        }
        return SkipResult.doNotSkip();
    }

    @SuppressWarnings("unchecked")
    protected Collection<Extension> getExtensionsFromTestSource() {
        return ((Stream<ExtendWith>) AnnotationUtils.streamRepeatableAnnotations(decorated.getAnnotatedElement(), ExtendWith.class))
                .map(ExtendWith::value)
                .flatMap(Arrays::stream)
                .map(ReflectionUtils::newInstanceOf)
                .collect(Collectors.toList());
    }

    // delegate methods

    @Override
    public UniqueId getUniqueId() {
        return decorated.getUniqueId();
    }

    @Override
    public String getDisplayName() {
        return decorated.getDisplayName();
    }

    @Override
    public Set<TestTag> getTags() {
        return decorated.getTags();
    }

    @Override
    public Optional<TestSource> getSource() {
        return decorated.getSource();
    }

    @Override
    public Optional<TestDescriptor> getParent() {
        return decorated.getParent();
    }

    @Override
    public void setParent(TestDescriptor parent) {
        decorated.setParent(parent);
    }

    @Override
    public Set<? extends TestDescriptor> getChildren() {
        return decorated.getChildren();
    }

    @Override
    public void addChild(TestDescriptor descriptor) {
        decorated.addChild(descriptor);
    }

    @Override
    public void removeChild(TestDescriptor descriptor) {
        decorated.removeChild(descriptor);
    }

    @Override
    public void removeFromHierarchy() {
        decorated.removeFromHierarchy();
    }

    @Override
    public Type getType() {
        return decorated.getType();
    }

    @Override
    public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId) {
        return decorated.findByUniqueId(uniqueId);
    }

    @Override
    public void createChildren(ElementResolver resolver) {
        decorated.createChildren(resolver);
    }
}
