package com.insa.hospital.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a SaaS roster limit is exceeded.
 *
 * AUDIT FIX — Alert 6: Patient/Doctor roster limit enforcement.
 *
 * Legacy source: doctor/controllers/doctor.php line 44
 *   $limit = $this->doctor_model->getLimit();
 *   if ($limit <= 0) { redirect with error; }
 *
 * p_limit (from hospital.p_limit) → max patients for this hospital
 * d_limit (from hospital.d_limit) → max doctors for this hospital
 *
 * Maps to HTTP 400 Bad Request to match frontend expectations.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LimitExceededException extends RuntimeException {

    public LimitExceededException(String message) {
        super(message);
    }

    public LimitExceededException(String resourceType, long current, long limit) {
        super(String.format(
            "%s roster limit reached (%d/%d). Upgrade your plan to add more %ss.",
            resourceType, current, limit, resourceType.toLowerCase()
        ));
    }
}
