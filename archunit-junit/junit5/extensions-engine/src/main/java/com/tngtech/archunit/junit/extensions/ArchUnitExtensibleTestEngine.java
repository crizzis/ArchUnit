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

import com.tngtech.archunit.junit.AbstractArchUnitTestDescriptor;
import com.tngtech.archunit.junit.AbstractArchUnitTestEngine;
import com.tngtech.archunit.junit.ArchUnitTestEngine;
import com.tngtech.archunit.junit.conditions.Conditions;

/**
 * An extension of the basic ArchUnit test engine that recognizes certain JUnit Jupiter annotations.
 * <br><br>
 * The currently supported annotations for method-flavored tests are: {@link org.junit.jupiter.api.Tag} and {@link org.junit.jupiter.api.Disabled},
 * as well as all versions of {@code @DisabledIfXxx} annotations from the {@link org.junit.jupiter.api.condition} package.
 *
 * Adapter annotations ({@link Conditions.DisabledIf}) are provided for field-flavored tests.
 * <br>
 * Since this engine is a drop-in replacement for the default {@link ArchUnitTestEngine}, test engines must be explicitly listed
 * in the configuration of a given test runner (excluding the default {@code archunit} identifier) to avoid conflict. <br>
 * <br>
 * Usage with Gradle: <br>
 * <pre><code>
 *     dependencies{
 *         testImplementation 'com.tngtech.archunit:archunit-junit5-extensions-engine:${archunit.version}'
 *     }
 *
 *     ...
 *      test {
 *          useJUnitPlatform {
 *              includeEngines 'archunit-extensible'
 *          }
 *      }
 * </code></pre>
 * <br>
 * Usage with Maven Surefire: <br>
 * <pre><code>
 *     &lt;dependency&gt;
 *         &lt;groupId&gt;com.tngtech.archunit&lt;/groupId&gt;
 *         &lt;artifactId&gt;archunit-junit5-extensions-engine&lt;/artifactId&gt;
 *         &lt;version&gt;${archunit.version}&lt;/version&gt;
 *         &lt;scope&gt;test&lt;/scope&gt; &lt;!-- or add as plugin dependency --&gt;
 *     &lt;/dependency&gt;
 *     ...
 *     &lt;plugin&gt;
 *         &lt;artifactId&gt;maven-surefire-plugin&lt;/artifactId&gt;
 *         &lt;configuration&gt;
 *             &lt;includeJUnit5Engines&gt;
 *                 &lt;engine>archunit-extensible&lt;/engine&gt;
 *             &lt;/includeJUnit5Engines&gt;
 *         &lt;/configuration&gt;
 *     &lt;/plugin&gt;
 * </code></pre>
 */
public final class ArchUnitExtensibleTestEngine
        extends AbstractArchUnitTestEngine<ExtensibleArchUnitEngineExecutionContext,
        ExtensibleTestDescriptorDecorator<AbstractArchUnitTestDescriptor<ExtensibleArchUnitEngineExecutionContext, ?>>> {

    static final String UNIQUE_ID = "archunit-extensible";

    private static final ExtensibleTestConfigurationFactory FACTORY = new ExtensibleTestConfigurationFactory();

    @Override
    public String getId() {
        return UNIQUE_ID;
    }

    public ArchUnitExtensibleTestEngine() {
        super(FACTORY, FACTORY);
    }
}
