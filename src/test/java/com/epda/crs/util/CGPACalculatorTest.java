package com.epda.crs.util;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CGPACalculatorTest {

    @Test
    void calculateReturnsWeightedAverage() {
        List<CGPACalculator.StudentResult> results = List.of(
                new CGPACalculator.StudentResult(4.0, 3),
                new CGPACalculator.StudentResult(3.0, 4),
                new CGPACalculator.StudentResult(2.0, 2)
        );
        double cgpa = CGPACalculator.calculate(results);
        Assertions.assertEquals(3.11, cgpa, 0.01);
    }

    @Test
    void calculateReturnsZeroForEmptyList() {
        double cgpa = CGPACalculator.calculate(List.of());
        Assertions.assertEquals(0.0, cgpa, 0.001);
    }

    @Test
    void calculateReturnsZeroForNullList() {
        double cgpa = CGPACalculator.calculate(null);
        Assertions.assertEquals(0.0, cgpa, 0.001);
    }
}
