package de.ferderer.matryoshka.example.violations;

import de.ferderer.matryoshka.example.policy.common.domain.PolicyDraft;

/**
 * VIOLATION V2: claims BC illegally accesses policy.common.
 * Expected: common_only_within_scope fires.
 * PolicyDraft lives in de.ferderer.matryoshka.example.policy.common — only de.ferderer.matryoshka.example.policy.. may access it.
 */
class ClaimsClassAccessingPolicyCommon {
    PolicyDraft illegalReference = null;
}
