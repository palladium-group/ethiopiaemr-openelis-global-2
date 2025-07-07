export const TestFormData = {
  testNameEnglish: "",
  testNameFrench: "",
  testReportNameEnglish: "",
  testReportNameFrench: "",
  testSection: "",
  panels: [],
  uom: "",
  loinc: "",
  resultType: "",
  orderable: "Y",
  notifyResults: "N",
  inLabOnly: "N",
  antimicrobialResistance: "N",
  active: "Y",
  dictionary: [],
  dictionaryReference: "",
  defaultTestResult: "",
  sampleTypes: [],
  lowValid: "-Infinity",
  highValid: "Infinity",
  lowReportingRange: "-Infinity",
  highReportingRange: "Infinity",
  lowCritical: "-Infinity",
  highCritical: "Infinity",
  significantDigits: "0",
  resultLimits: [
    {
      ageRange: "0",
      highAgeRange: "0",
      gender: false,
      lowNormal: "-Infinity",
      highNormal: "Infinity",
      lowNormalFemale: "-Infinity",
      highNormalFemale: "Infinity",
    },
  ],
};

export const mapTestCatBeanToFormData = (test) => {
  return {
    testId: test.id,
    testNameEnglish: test.localization?.english || "",
    testNameFrench: test.localization?.french || "",
    testReportNameEnglish: test.reportLocalization?.english || "",
    testReportNameFrench: test.reportLocalization?.french || "",
    testSection: test.testUnit || "",
    panels: test.panel && test.panel !== "None" ? [test.panel] : [],
    uom: test.uom || "",
    loinc: test.loinc || "",
    resultType: test.resultType || "",
    orderable: test.orderable === "Orderable" ? "Y" : "N",
    notifyResults: test.notifyResults ? "Y" : "N",
    inLabOnly: test.inLabOnly ? "Y" : "N",
    antimicrobialResistance: test.antimicrobialResistance ? "Y" : "N",
    active: test.active === "Active" ? "Y" : "N",
    dictionary: test.dictionaryValues || [],
    dictionaryReference: test.referenceValue || "",
    defaultTestResult: "",
    sampleTypes: test.sampleType ? [test.sampleType] : [],
    lowValid: "-Infinity", // this may be needs to fetched from resultLimits collection
    highValid: "Infinity",
    lowReportingRange: "-Infinity",
    highReportingRange: "Infinity",
    lowCritical: "-Infinity",
    highCritical: "Infinity",
    significantDigits:
      test.significantDigits !== "n/a" ? test.significantDigits : "0",
    resultLimits:
      test.resultLimits?.map((limit) => {
        const normalRange = limit.normalRange?.split("-") || [];
        return {
          ageRange: limit.ageRange || "0",
          highAgeRange: "0",
          gender:
            limit.gender === "n/a"
              ? false
              : limit.gender === "M"
                ? true
                : limit.gender === "F"
                  ? true
                  : false,
          lowNormal: normalRange[0] || "-Infinity",
          highNormal: normalRange[1] || "Infinity",
          lowNormalFemale: normalRange[0] || "-Infinity",
          highNormalFemale: normalRange[1] || "Infinity",
        };
      }) || [],
  };
};

// {
//     "id": "391",
//     "localization": {
//         "lastupdated": 1751779732760,
//         "id": "582",
//         "description": "test name",
//         "localeValues": {
//             "fr": "demoi",
//             "en": "demoi"
//         },
//         "localesWithValueSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesAndValuesOfLocalesWithValues": [
//             "English: demoi",
//             "French: demoi"
//         ],
//         "localizedValue": "demoi",
//         "localesWithValue": [
//             "en",
//             "fr"
//         ],
//         "english": "demoi",
//         "french": "demoi",
//         "allActiveLocales": [
//             "en",
//             "fr"
//         ],
//         "localesSortedForDisplay": [
//             "en",
//             "fr"
//         ]
//     },
//     "reportLocalization": {
//         "lastupdated": 1751779732799,
//         "id": "583",
//         "description": "test report name",
//         "localeValues": {
//             "fr": "demoi",
//             "en": "demoi"
//         },
//         "localesWithValueSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesAndValuesOfLocalesWithValues": [
//             "English: demoi",
//             "French: demoi"
//         ],
//         "localizedValue": "demoi",
//         "localesWithValue": [
//             "en",
//             "fr"
//         ],
//         "english": "demoi",
//         "french": "demoi",
//         "allActiveLocales": [
//             "en",
//             "fr"
//         ],
//         "localesSortedForDisplay": [
//             "en",
//             "fr"
//         ]
//     },
//     "testUnit": "Biochemistry",
//     "sampleType": "Sputum",
//     "panel": "Bilan Biochimique",
//     "resultType": "N",
//     "uom": "micron^3",
//     "significantDigits": "1",
//     "loinc": "1",
//     "active": "Active",
//     "orderable": "Orderable",
//     "notifyResults": true,
//     "hasDictionaryValues": false,
//     "hasLimitValues": true,
//     "resultLimits": [
//         {
//             "gender": "M",
//             "ageRange": "0D/0M/0Y-0D/10M/0Y",
//             "normalRange": "1.0-2.0",
//             "validRange": "1.0-2.0",
//             "reportingRange": "1.0-2.0",
//             "criticalRange": "1.0-2.0"
//         },
//         {
//             "gender": "F",
//             "ageRange": "0D/0M/0Y-0D/10M/0Y",
//             "normalRange": "1.0-2.0",
//             "validRange": "1.0-2.0",
//             "reportingRange": "1.0-2.0",
//             "criticalRange": "1.0-2.0"
//         },
//         {
//             "gender": "M",
//             "ageRange": "0D/10M/0Y-25D/7M/1Y",
//             "normalRange": "Any value",
//             "validRange": "1.0-2.0",
//             "reportingRange": "1.0-2.0",
//             "criticalRange": "1.0-2.0"
//         },
//         {
//             "gender": "F",
//             "ageRange": "0D/10M/0Y-25D/7M/1Y",
//             "normalRange": "Any value",
//             "validRange": "1.0-2.0",
//             "reportingRange": "1.0-2.0",
//             "criticalRange": "1.0-2.0"
//         },
//         {
//             "gender": "n/a",
//             "ageRange": "0D/10M/0Y-20D/5M/2Y",
//             "normalRange": "1.0-2.0",
//             "validRange": "1.0-2.0",
//             "reportingRange": "1.0-2.0",
//             "criticalRange": "1.0-2.0"
//         },
//         {
//             "gender": "n/a",
//             "ageRange": "25D/7M/1Y-0D/10M/0Y",
//             "normalRange": "Any value",
//             "validRange": "1.0-2.0",
//             "reportingRange": "1.0-2.0",
//             "criticalRange": "1.0-2.0"
//         }
//     ],
//     "testSortOrder": 13,
//     "inLabOnly": true,
//     "antimicrobialResistance": true
// }

// {
//     "testNameEnglish": "demoi",
//     "testNameFrench": "demoi",
//     "testReportNameEnglish": "demoi",
//     "testReportNameFrench": "demoi",
//     "testSection": "Biochemistry",
//     "panels": [
//         "Bilan Biochimique"
//     ],
//     "uom": "micron^3",
//     "loinc": "1",
//     "resultType": "N",
//     "orderable": "Y",
//     "notifyResults": "Y",
//     "inLabOnly": "Y",
//     "antimicrobialResistance": "Y",
//     "active": "Y",
//     "dictionary": [],
//     "dictionaryReference": "",
//     "defaultTestResult": "",
//     "sampleTypes": [
//         "Sputum"
//     ],
//     "lowValid": "-Infinity",
//     "highValid": "Infinity",
//     "lowReportingRange": "-Infinity",
//     "highReportingRange": "Infinity",
//     "lowCritical": "-Infinity",
//     "highCritical": "Infinity",
//     "significantDigits": "1",
//     "resultLimits": [
//         {
//             "ageRange": "0D/0M/0Y-0D/10M/0Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "1.0",
//             "highNormal": "2.0",
//             "lowNormalFemale": "1.0",
//             "highNormalFemale": "2.0"
//         },
//         {
//             "ageRange": "0D/0M/0Y-0D/10M/0Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "1.0",
//             "highNormal": "2.0",
//             "lowNormalFemale": "1.0",
//             "highNormalFemale": "2.0"
//         },
//         {
//             "ageRange": "0D/10M/0Y-25D/7M/1Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         },
//         {
//             "ageRange": "0D/10M/0Y-25D/7M/1Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         },
//         {
//             "ageRange": "0D/10M/0Y-20D/5M/2Y",
//             "highAgeRange": "0",
//             "gender": false,
//             "lowNormal": "1.0",
//             "highNormal": "2.0",
//             "lowNormalFemale": "1.0",
//             "highNormalFemale": "2.0"
//         },
//         {
//             "ageRange": "25D/7M/1Y-0D/10M/0Y",
//             "highAgeRange": "0",
//             "gender": false,
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         }
//     ]
// }

// [
//     {
//         "ageRange": "1",
//         "highAgeRange": "0",
//         "gender": true,
//         "lowNormal": "1",
//         "highNormal": "2",
//         "lowNormalFemale": "1",
//         "highNormalFemale": "2"
//     },
//     {
//         "ageRange": "12",
//         "highAgeRange": "0",
//         "gender": true,
//         "lowNormal": "-Infinity",
//         "highNormal": "Infinity",
//         "lowNormalFemale": "-Infinity",
//         "highNormalFemale": "Infinity"
//     },
//     {
//         "ageRange": "60",
//         "highAgeRange": "0",
//         "gender": false,
//         "lowNormal": "-Infinity",
//         "highNormal": "Infinity",
//         "lowNormalFemale": "-Infinity",
//         "highNormalFemale": "Infinity"
//     },
//     {
//         "ageRange": "168",
//         "highAgeRange": "0",
//         "gender": false,
//         "lowNormal": "1",
//         "highNormal": "2",
//         "lowNormalFemale": "-Infinity",
//         "highNormalFemale": "Infinity"
//     }
// ]
