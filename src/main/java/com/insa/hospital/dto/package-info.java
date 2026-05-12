/**
 * DTO (Data Transfer Object) package.
 *
 * Contains request and response DTOs for each API endpoint.
 * DTOs deliberately do NOT expose internal entity structure or legacy field names
 * that are ambiguous (e.g. 'x', 'y', 'z'). API consumers see clean field names
 * while the service layer maps between DTO fields and legacy entity columns.
 *
 * Naming convention: <EntityName>Request.java / <EntityName>Response.java
 */
package com.insa.hospital.dto;
