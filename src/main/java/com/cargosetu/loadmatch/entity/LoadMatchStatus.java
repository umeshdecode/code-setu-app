package com.cargosetu.loadmatch.entity;

/**
 * Full 6-state lifecycle for a LoadMatch suggestion.
 *
 * Phase 2 scope note: only the SUGGESTED -> VIEWED -> EXPIRED path is
 * implemented by service/controller code in this phase. REQUESTED, ACCEPTED
 * and REJECTED are declared here so Phase 3 (which adds the transactional
 * capacity-locking request/accept/reject flow) does not need a migration to
 * add enum values later - but nothing in this codebase transitions a
 * LoadMatch into or out of those three states yet.
 */
public enum LoadMatchStatus {
    SUGGESTED,
    VIEWED,
    REQUESTED,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
