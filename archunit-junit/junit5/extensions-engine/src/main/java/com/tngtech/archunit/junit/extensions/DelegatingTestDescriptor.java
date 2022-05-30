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

import java.util.Optional;
import java.util.Set;

import com.tngtech.archunit.junit.CreatesChildren;
import com.tngtech.archunit.junit.ElementResolver;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.ExclusiveResource;
import org.junit.platform.engine.support.hierarchical.Node;

abstract class DelegatingTestDescriptor
        <T extends EngineExecutionContext, D extends TestDescriptor & Node<T> & CreatesChildren>
        implements TestDescriptor, Node<T>, CreatesChildren {

    private final D decorated;

    DelegatingTestDescriptor(D decorated) {
        this.decorated = decorated;
    }

    D getDecorated() {
        return decorated;
    }

    @Override
    public UniqueId getUniqueId() {
        return decorated.getUniqueId();
    }

    @Override
    public String getDisplayName() {
        return decorated.getDisplayName();
    }

    @Override
    public String getLegacyReportingName() {
        return decorated.getLegacyReportingName();
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
    public Set<? extends TestDescriptor> getDescendants() {
        return decorated.getDescendants();
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
    public boolean isRoot() {
        return decorated.isRoot();
    }

    @Override
    public Type getType() {
        return decorated.getType();
    }

    @Override
    public boolean isContainer() {
        return decorated.isContainer();
    }

    @Override
    public boolean isTest() {
        return decorated.isTest();
    }

    @Override
    public boolean mayRegisterTests() {
        return decorated.mayRegisterTests();
    }

    @Override
    public void prune() {
        decorated.prune();
    }

    @Override
    public Optional<? extends TestDescriptor> findByUniqueId(UniqueId uniqueId) {
        return decorated.findByUniqueId(uniqueId);
    }

    @Override
    public void accept(Visitor visitor) {
        decorated.accept(visitor);
    }

    @Override
    public void createChildren(ElementResolver resolver) {
        decorated.createChildren(resolver);
    }

    @Override
    public T prepare(T context) throws Exception {
        return decorated.prepare(context);
    }

    @Override
    public void cleanUp(T context) throws Exception {
        decorated.cleanUp(context);
    }

    @Override
    public SkipResult shouldBeSkipped(T context) throws Exception {
        return decorated.shouldBeSkipped(context);
    }

    @Override
    public T before(T context) throws Exception {
        return decorated.before(context);
    }

    @Override
    public T execute(T context, DynamicTestExecutor dynamicTestExecutor) throws Exception {
        return decorated.execute(context, dynamicTestExecutor);
    }

    @Override
    public void after(T context) throws Exception {
        decorated.after(context);
    }

    @Override
    public void around(T context, Invocation<T> invocation) throws Exception {
        decorated.around(context, invocation);
    }

    @Override
    public void nodeSkipped(T context, TestDescriptor testDescriptor, SkipResult result) {
        decorated.nodeSkipped(context, testDescriptor, result);
    }

    @Override
    public void nodeFinished(T context, TestDescriptor testDescriptor, TestExecutionResult result) {
        decorated.nodeFinished(context, testDescriptor, result);
    }

    @Override
    public Set<ExclusiveResource> getExclusiveResources() {
        return decorated.getExclusiveResources();
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return decorated.getExecutionMode();
    }
}
