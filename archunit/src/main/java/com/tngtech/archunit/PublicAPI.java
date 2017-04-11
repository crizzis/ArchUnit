package com.tngtech.archunit;

import java.lang.annotation.Inherited;

/**
 * Marks classes and members, that are part of ArchUnit's public API. I.e. users of ArchUnit should ONLY use
 * those classes and members.<br><br>
 * Furthermore the specified {@link #usage()} defines the way, this public API should be used.<br>
 * {@link Usage#ACCESS} defines, that this class or member should only be accessed (e.g. calling a method)
 * by users of ArchUnit. {@link Usage#INHERITANCE} defines, that this class / interface may be extended / implemented
 * by users of ArchUnit. Note that this naturally includes permission to access any accessible members
 * of this class / interface.<br><br>
 * Any usage of ArchUnit's classes outside of this contract, is not supported and may break with any (even minor)
 * release.
 */
@Internal
@Inherited
public @interface PublicAPI {
    Usage usage();

    @Internal
    enum Usage {
        INHERITANCE,
        ACCESS
    }
}
