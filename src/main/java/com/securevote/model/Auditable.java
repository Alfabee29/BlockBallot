package com.securevote.model;

/**
 * INTERFACE — defines the contract for any auditable entity.
 *
 * OOP Concept: Abstraction + Interface
 * • Hides implementation detail of HOW integrity is verified.
 * • Any class implementing this MUST provide its own logic.
 * • Enables polymorphism — we can call verifyIntegrity() on
 * any Auditable object without knowing its concrete type.
 */
public interface Auditable {

    /**
     * Generate a cryptographic hash of this entity's contents.
     * 
     * @return hex-encoded hash string
     */
    String generateCryptographicHash();

    /**
     * Verify that this entity has not been tampered with.
     * 
     * @return true if the entity's stored hash matches recalculation
     */
    boolean verifyIntegrity();

    /**
     * Return a human-readable audit summary.
     * 
     * @return description of this auditable entity
     */
    default String getAuditSummary() {
        return getClass().getSimpleName()
                + " [integrity=" + (verifyIntegrity() ? "VALID" : "TAMPERED") + "]";
    }
}