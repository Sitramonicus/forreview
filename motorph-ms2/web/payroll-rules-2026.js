// Temporary, versioned statutory rules for the 2026 monthly preview.
// Keep formula/rate changes here; do not turn this module into production policy
// without independent review and approved worked examples.
export const PAYROLL_RULES_2026 = Object.freeze({
  id: 'PH-2026-monthly-preview-v1',
  effectiveYear: 2026,
  rounding: 'half-up to centavo after each statutory line',
  sss: Object.freeze({
    minimumMscPesos: 5000,
    maximumMscPesos: 35000,
    firstHigherRangePesos: 5250,
    rangeStepPesos: 500,
    mscStepPesos: 500,
    employeeRateBasisPoints: 500,
    employerRateBasisPoints: 1000,
    ecThresholdMscPesos: 15000,
    ecBelowThresholdPesos: 10,
    ecAtOrAboveThresholdPesos: 30
  }),
  philHealth: Object.freeze({
    premiumRateBasisPoints: 500,
    baseFloorPesos: 10000,
    baseCeilingPesos: 100000,
    employeeShareBasisPoints: 5000,
    employerShareBasisPoints: 5000
  }),
  pagIbig: Object.freeze({
    fundSalaryCeilingPesos: 10000,
    employeeLowBandCeilingPesos: 1500,
    employeeLowRateBasisPoints: 100,
    employeeHighRateBasisPoints: 200,
    employerRateBasisPoints: 200
  }),
  birMonthly: Object.freeze({
    // BIR Annex E monthly withholding table, effective 2023-01 onward.
    // `floorPesos` is the threshold above which the marginal rate applies.
    // Upper bounds are exclusive; fixed base taxes follow Annex E as printed.
    firstTaxablePesoThreshold: 20833,
    brackets: Object.freeze([
      Object.freeze({ belowPesos: '33333', floorPesos: '20833', baseTaxPesos: '0.00', rateBasisPoints: 1500 }),
      Object.freeze({ belowPesos: '66667', floorPesos: '33333', baseTaxPesos: '1875.00', rateBasisPoints: 2000 }),
      Object.freeze({ belowPesos: '166667', floorPesos: '66667', baseTaxPesos: '8541.80', rateBasisPoints: 2500 }),
      Object.freeze({ belowPesos: '666667', floorPesos: '166667', baseTaxPesos: '33541.80', rateBasisPoints: 3000 }),
      Object.freeze({ belowPesos: null, floorPesos: '666667', baseTaxPesos: '183541.80', rateBasisPoints: 3500 })
    ])
  }),
  sources: Object.freeze({
    sss: 'SSS Circular No. 2024-006 and 2025 contribution table, effective 2025-01-01',
    philHealth: 'PhilHealth Advisory PA2025-0002, 5% premium effective 2025-01, 2025 schedule',
    pagIbig: 'Pag-IBIG Fund Circular No. 460, effective 2024-02',
    bir: 'BIR RR 11-2018 Annex E, withholding table effective 2023-01 onward'
  })
});
