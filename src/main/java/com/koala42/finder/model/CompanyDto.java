package com.koala42.finder.model;

public record CompanyDto(
        String ico,
        String name,
        String municipality,
        String address,
        String legalForm,
        String established,
        String terminated,
        String employeeCategory,
        String nace,
        String dataValidAsOf,
        String source
) {}
