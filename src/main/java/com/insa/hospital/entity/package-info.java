/**
 * Entity package.
 *
 * Contains JPA @Entity classes that map 1:1 to legacy MySQL tables.
 *
 * CRITICAL RULES:
 * - Every entity uses @Table(name="<exact_legacy_table_name>")
 * - Every field uses @Column(name="<exact_legacy_column_name>")
 * - No new columns or tables are created — ddl-auto=none enforces this.
 * - All date/time fields are mapped as String (legacy stores as Unix epoch strings).
 * - All FK-like fields (hospital_id, patient, doctor, etc.) are mapped as String.
 */
package com.insa.hospital.entity;
