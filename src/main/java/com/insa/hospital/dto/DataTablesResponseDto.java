package com.insa.hospital.dto;

import java.util.List;

/**
 * Common DataTables Output format.
 * 
 * Required natively by the legacy jQuery DataTables plugin.
 * Mirrors the Exact JSON returned by:
 * $output = array("draw" => intval($_REQUEST['draw']), "recordsTotal" => recordsTotal, "recordsFiltered" => recordsTotal, "data" => info);
 */
public record DataTablesResponseDto(
        int draw,
        long recordsTotal,
        long recordsFiltered,
        List<List<Object>> data
) {
    public static DataTablesResponseDto of(int draw, long total, List<List<Object>> data) {
        return new DataTablesResponseDto(draw, total, total, data);
    }
}
