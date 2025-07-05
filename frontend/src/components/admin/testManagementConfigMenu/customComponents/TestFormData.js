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
//     "testNameEnglish": "demob",
//     "testNameFrench": "demob",
//     "testReportNameEnglish": "demob",
//     "testReportNameFrench": "demob",
//     "testSection": "Biochemistry",
//     "panels": [
//         "Bilan Biochimique"
//     ],
//     "uom": "mg/dl",
//     "loinc": "123",
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
//             "ageRange": "0D/0M/0Y-0D/0M/10Y",
//             "highAgeRange": "0",
//             "gender": "M",
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         },
//         {
//             "ageRange": "0D/0M/0Y-0D/0M/10Y",
//             "highAgeRange": "0",
//             "gender": "F",
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         }
//     ]
// }
