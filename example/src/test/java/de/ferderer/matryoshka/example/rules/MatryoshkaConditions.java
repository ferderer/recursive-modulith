package de.ferderer.matryoshka.example.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

final class MatryoshkaConditions {

    private MatryoshkaConditions() {}

    /**
     * Enforces that a class in a {@code config} package resides at the top level only —
     * i.e. its package is exactly {@code <rootPackage>.config} or {@code <rootPackage>.config.<sub>},
     * but never {@code <rootPackage>.<bc>.config..}.
     *
     * Root is detected as the package that contains no {@code config} segment before the last one.
     * Concretely: the package must not contain any non-config, non-root segment between root and config.
     *
     * Implementation: split the package on ".config" — the left side must not contain
     * any further dots beyond the root package (i.e. no BC segment before config).
     */
    static ArchCondition<JavaClass> resideInTopLevelConfigPackage(String rootPackage) {
        String expectedPrefix = rootPackage + ".config";
        return new ArchCondition<>("reside in top-level config package '" + expectedPrefix + "'") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String pkg = clazz.getPackageName();
                boolean isTopLevel = pkg.equals(expectedPrefix) || pkg.startsWith(expectedPrefix + ".");
                if (!isTopLevel) {
                    events.add(SimpleConditionEvent.violated(clazz,
                        clazz.getName()
                        + " resides in '" + pkg + "'"
                        + " but config must only exist directly under '" + rootPackage + "'"
                        + " in (" + clazz.getSource().map(Object::toString).orElse("unknown") + ")"
                    ));
                }
            }
        };
    }

    /**
     * Enforces that classes in any {@code common} package are only accessed
     * from within the same scope — i.e. the package tree rooted at the parent
     * of {@code common}.
     *
     * Example:
     * {@code de.ferderer.matryoshka.example.claims.common.domain.ClaimEntity}
     * may only be accessed by classes in
     * {@code de.ferderer.matryoshka.example.claims..}
     */
    static ArchCondition<JavaClass> onlyBeAccessedFromWithinScope() {
        return new ArchCondition<>("only be accessed from within their own scope") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String pkg = clazz.getPackageName();
                int commonIdx = pkg.lastIndexOf(".common");
                if (commonIdx == -1) return;
                String allowedPrefix = pkg.substring(0, commonIdx);

                clazz.getAccessesToSelf().forEach(access -> {
                    String callerPkg = access.getOriginOwner().getPackageName();

                    // Derive the caller's scope the same way — strip everything from .common onward
                    int callerCommonIdx = callerPkg.lastIndexOf(".common");
                    String callerScope = callerCommonIdx != -1
                        ? callerPkg.substring(0, callerCommonIdx)
                        : callerPkg;

                    boolean withinScope = callerScope.equals(allowedPrefix)
                        || callerScope.startsWith(allowedPrefix + ".");
                    if (!withinScope) {
                        events.add(SimpleConditionEvent.violated(access,
                            access.getOriginOwner().getName()
                            + " accesses " + clazz.getName()
                            + " but resides outside of scope '" + allowedPrefix + "'"
                            + " in (" + access.getSourceCodeLocation() + ")"
                        ));
                    }
                });
            }
        };
    }
}
