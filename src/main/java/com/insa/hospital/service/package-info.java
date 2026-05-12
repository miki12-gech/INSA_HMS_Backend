/**
 * Service package.
 *
 * Contains @Service classes implementing business logic.
 * Each service class corresponds to a module (patient, doctor, lab, etc.)
 * and orchestrates calls to one or more repositories.
 *
 * All services receive the hospitalId from the SecurityContext (JWT claims)
 * and apply it as a filter on every query to enforce multi-tenancy.
 *
 * Pattern: <ModuleName>Service.java — interface + <ModuleName>ServiceImpl.java
 */
package com.insa.hospital.service;
