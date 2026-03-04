package com.sonnk.user.model.entity;

import com.sonnk.user.model.entity.enums.EmploymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
@Getter
@Setter
public class EmployeeModel extends UserModel {

    @Column(nullable = false, length = 50)
    private String branchCode;

    @Column(length = 100)
    private String position; // barista, cashier, manager...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;
}
