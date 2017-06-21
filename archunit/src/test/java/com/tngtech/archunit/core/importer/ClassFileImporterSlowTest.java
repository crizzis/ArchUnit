package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.Slow;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.testutil.TransientCopyRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.tngtech.archunit.core.domain.SourceTest.urlOf;
import static com.tngtech.archunit.core.importer.ClassFileImporterTest.jarFileOf;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;

@Category(Slow.class)
public class ClassFileImporterSlowTest {
    @Rule
    public final TransientCopyRule copyRule = new TransientCopyRule();

    @Test
    public void imports_the_classpath() {
        JavaClasses classes = new ClassFileImporter().importClasspath();

        assertThatClasses(classes).contain(ClassFileImporter.class, getClass());
        assertThatClasses(classes).dontContain(Rule.class); // Default doesn't import jars

        classes = new ClassFileImporter().importClasspath(new ImportOptions());

        assertThatClasses(classes).contain(ClassFileImporter.class, getClass(), Rule.class);
    }

    @Test
    public void imports_jars() throws Exception {
        JavaClasses classes = new ClassFileImporter().importJar(jarFileOf(Rule.class));
        assertThatClasses(classes).contain(Rule.class);
        assertThatClasses(classes).dontContain(Object.class, ImmutableList.class);

        classes = new ClassFileImporter().importJars(jarFileOf(Rule.class), jarFileOf(ImmutableList.class));
        assertThatClasses(classes).contain(Rule.class, ImmutableList.class);
        assertThatClasses(classes).dontContain(Object.class);

        classes = new ClassFileImporter().importJars(ImmutableList.of(
                jarFileOf(Rule.class), jarFileOf(ImmutableList.class)));
        assertThatClasses(classes).contain(Rule.class, ImmutableList.class);
        assertThatClasses(classes).dontContain(Object.class);
    }

    @Test
    public void imports_duplicate_classes() throws IOException {
        String existingClass = urlOf(JavaClass.class).getFile();
        copyRule.copy(
                new File(existingClass),
                new File(getClass().getResource(".").getFile()));

        JavaClasses classes = new ClassFileImporter().importPackages(getClass().getPackage().getName());

        assertThat(classes.get(JavaClass.class)).isNotNull();
    }
}
