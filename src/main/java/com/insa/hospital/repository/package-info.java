/**
 * Repository package.
 *
 * Contains Spring Data JPA repositories (extending JpaRepository or CrudRepository).
 * Each repository maps to a corresponding @Entity in the entity package.
 *
 * Pattern: <EntityName>Repository extends JpaRepository<<EntityName>, Integer>
 *
 * IMPORTANT: When writing @Query annotations, use JPQL entity/field names (Java names),
 * NOT the legacy SQL column names. The @Column mapping handles the translation.
 */
package com.insa.hospital.repository;
