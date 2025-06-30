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
          gender: limit.gender === "n/a" ? false : limit.gender,
          lowNormal: normalRange[0] || "-Infinity",
          highNormal: normalRange[1] || "Infinity",
          lowNormalFemale: normalRange[0] || "-Infinity",
          highNormalFemale: normalRange[1] || "Infinity",
        };
      }) || [],
  };
};

// {
//     "id": "38",
//     "localization": {
//         "lastupdated": 1579758371217,
//         "id": "77",
//         "description": "test name",
//         "localeValues": {
//             "fr": "Detection de la resistance aux antiretroviraux",
//             "en": "ARV resistance"
//         },
//         "localizedValue": "ARV resistance",
//         "localesWithValue": [
//             "en",
//             "fr"
//         ],
//         "english": "ARV resistance",
//         "french": "Detection de la resistance aux antiretroviraux",
//         "allActiveLocales": [
//             "en",
//             "fr"
//         ],
//         "localesSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesWithValueSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesAndValuesOfLocalesWithValues": [
//             "English: ARV resistance",
//             "French: Detection de la resistance aux antiretroviraux"
//         ]
//     },
//     "reportLocalization": {
//         "lastupdated": 1579758371217,
//         "id": "78",
//         "description": "test report name",
//         "localeValues": {
//             "fr": "ARV res",
//             "en": "ARV resistance"
//         },
//         "localizedValue": "ARV resistance",
//         "localesWithValue": [
//             "en",
//             "fr"
//         ],
//         "english": "ARV resistance",
//         "french": "ARV res",
//         "allActiveLocales": [
//             "en",
//             "fr"
//         ],
//         "localesSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesWithValueSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesAndValuesOfLocalesWithValues": [
//             "English: ARV resistance",
//             "French: ARV res"
//         ]
//     },
//     "testUnit": "Molecular Biology",
//     "sampleType": "Whole Blood",
//     "panel": "None",
//     "resultType": "N",
//     "uom": "copies/ml",
//     "significantDigits": "0",
//     "active": "Active",
//     "orderable": "Orderable",
//     "notifyResults": false,
//     "hasDictionaryValues": false,
//     "hasLimitValues": true,
//     "resultLimits": [
//         {
//             "gender": "n/a",
//             "ageRange": "Any Age",
//             "normalRange": "1-300",
//             "validRange": "1-200000",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         }
//     ],
//     "testSortOrder": 380,
//     "inLabOnly": false,
//     "antimicrobialResistance": false
// }

// {
//   "id": "12",
//   "localization": {
//     "lastupdated": 1579758371217,
//     "id": "25",
//     "description": "test name",
//     "localeValues": {
//       "fr": "Protéinurie sur bandelette",
//       "en": "Proteinuria dipstick"
//     },
//     "localizedValue": "Proteinuria dipstick",
//     "localesWithValue": [
//       "en",
//       "fr"
//     ],
//     "english": "Proteinuria dipstick",
//     "french": "Protéinurie sur bandelette",
//     "allActiveLocales": [
//       "en",
//       "fr"
//     ],
//     "localesSortedForDisplay": [
//       "en",
//       "fr"
//     ],
//     "localesWithValueSortedForDisplay": [
//       "en",
//       "fr"
//     ],
//     "localesAndValuesOfLocalesWithValues": [
//       "English: Proteinuria dipstick",
//       "French: Protéinurie sur bandelette"
//     ]
//   },
//   "reportLocalization": {
//     "lastupdated": 1579758371217,
//     "id": "26",
//     "description": "test report name",
//     "localeValues": {
//       "fr": "Protéines",
//       "en": "Proteinuria dipstick"
//     },
//     "localizedValue": "Proteinuria dipstick",
//     "localesWithValue": [
//       "en",
//       "fr"
//     ],
//     "english": "Proteinuria dipstick",
//     "french": "Protéines",
//     "allActiveLocales": [
//       "en",
//       "fr"
//     ],
//     "localesSortedForDisplay": [
//       "en",
//       "fr"
//     ],
//     "localesWithValueSortedForDisplay": [
//       "en",
//       "fr"
//     ],
//     "localesAndValuesOfLocalesWithValues": [
//       "English: Proteinuria dipstick",
//       "French: Protéines"
//     ]
//   },
//   "testUnit": "Biochemistry",
//   "sampleType": "Urines",
//   "panel": "None",
//   "resultType": "D",
//   "uom": "n/a",
//   "significantDigits": "n/a",
//   "active": "Active",
//   "orderable": "Orderable",
//   "notifyResults": false,
//   "hasDictionaryValues": true,
//   "dictionaryValues": [
//     "Negative",
//     "Positive +++",
//     "Positive ++",
//     "Positive +",
//     "Negative"
//   ],
//   "dictionaryIds": [
//     "1103",
//     "1102",
//     "1101",
//     "1100",
//     "1103"
//   ],
//   "referenceValue": "n/a",
//   "hasLimitValues": false,
//   "testSortOrder": 120,
//   "inLabOnly": false,
//   "antimicrobialResistance": false
// },
