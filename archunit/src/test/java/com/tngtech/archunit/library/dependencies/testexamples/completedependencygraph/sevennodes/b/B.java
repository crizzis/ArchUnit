package com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.b;

import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.a.A;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.c.C;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.d.D;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.e.E;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.f.F;
import com.tngtech.archunit.library.dependencies.testexamples.completedependencygraph.sevennodes.g.G;

@SuppressWarnings("unused")
public class B {
    private A a;
    private B b;
    private C c;
    private D d;
    private E e;
    private F f;
    private G g;
}
