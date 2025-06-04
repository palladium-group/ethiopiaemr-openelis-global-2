import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
  Select,
  SelectItem,
  Stack,
  TextInput,
  Checkbox,
  Row,
  FlexGrid,
  Tag,
  UnorderedList,
  ListItem,
  NumberInput,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  ClickableTile,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { Formik, Form } from "formik";
import * as Yup from "yup";
import { CustomShowGuide } from "./customComponents/CustomShowGuide.js";
import { CustomCommonSortableOrderList } from "./sortableListComponent/SortableList.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.test.add",
    link: "/MasterListsPage#TestAdd",
  },
];

function TestAdd() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [lonic, setLonic] = useState("");
  const [testAdd, setTestAdd] = useState({});
  const [ageRangeList, setAgeRangeList] = useState([]);
  const [gotSelectedAgeRangeList, setGotSelectedAgeRangeList] = useState([]);
  const [labUnitList, setLabUnitList] = useState([]);
  const [selectedLabUnitList, setSelectedLabUnitList] = useState({});
  const [panelList, setPanelList] = useState([]);
  const [panelListTag, setPanelListTag] = useState([]);
  const [uomList, setUomList] = useState([]);
  const [selectedUomList, setSelectedUomList] = useState({});
  const [resultTypeList, setResultTypeList] = useState([]);
  const [selectedResultTypeList, setSelectedResultTypeList] = useState({});
  const [sampleTypeList, setSampleTypeList] = useState([]);
  const [selectedSampleTypeList, setSelectedSampleTypeList] = useState([]);
  const [sampleTestTypeToGetTagList, setSampleTestTypeToGetTagList] = useState(
    [],
  );
  const [selectedSampleType, setSelectedSampleType] = useState([]);
  const [selectedSampleTypeResp, setSelectedSampleTypeResp] = useState([]);
  const [groupedDictionaryList, setGroupedDictionaryList] = useState([]);
  const [selectedGroupedDictionaryList, setSelectedGroupedDictionaryList] =
    useState([]);
  const [dictionaryList, setDictionaryList] = useState([]);
  const [dictionaryListTag, setDictionaryListTag] = useState([]);
  const [singleSelectDictionaryList, setSingleSelectDictionaryList] = useState(
    [],
  );
  const [multiSelectDictionaryList, setMultiSelectDictionaryList] = useState(
    [],
  );
  const [multiSelectDictionaryListTag, setMultiSelectDictionaryListTag] =
    useState([]);
  const [jsonWad, setJsonWad] = useState(
    // {
    //   testNameEnglish: "aasdf",
    //   testNameFrench: "asdf",
    //   testReportNameEnglish: "aasdf",
    //   testReportNameFrench: "asdf",
    //   testSection: "56",
    //   panels: [{ id: "1" }, { id: "2" }],
    //   uom: "1",
    //   loinc: "asdf",
    //   resultType: "4",
    //   orderable: "Y",
    //   notifyResults: "Y",
    //   inLabOnly: "Y",
    //   antimicrobialResistance: "Y",
    //   active: "Y",
    //   sampleTypes:
    //     '[{"typeId": "31", "tests": [{"id": 301}, {"id": 0}]}, {"typeId": "3", "tests": [{"id": 306}, {"id": 304}, {"id": 308}, {"id": 319}, {"id": 317}, {"id": 311}, {"id": 314}, {"id": 3}, {"id": 32}, {"id": 40}, {"id": 41}, {"id": 56}, {"id": 47}, {"id": 49}, {"id": 51}, {"id": 0}]}]',
    //   lowValid: "-Infinity",
    //   highValid: "Infinity",
    //   lowReportingRange: "-Infinity",
    //   highReportingRange: "Infinity",
    //   lowCritical: "-Infinity",
    //   highCritical: "-Infinity",
    //   significantDigits: "",
    //   resultLimits:
    //     '[{"highAgeRange": "30", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "365", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "1825", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "5110", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "Infinity", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}]',
    //   dictionary:
    //     '[{"value": "824", "qualified": "N"}, {"value": "826", "qualified": "N"}, {"value": "825", "qualified": "N"}, {"value": "822", "qualified": "N"}, {"value": "829", "qualified": "N"}, {"value": "821", "qualified": "N"}]',
    //   dictionaryReference: "824",
    //   defaultTestResult: "825",
    // },
    {
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
      notifyResults: "",
      inLabOnly: "",
      antimicrobialResistance: "",
      active: "Y",
      sampleTypes: [],
      lowValid: "",
      highValid: "",
      lowReportingRange: "",
      highReportingRange: "",
      lowCritical: "",
      highCritical: "",
      significantDigits: "",
      resultLimits: [
        {
          highAgeRange: 30,
          gender: false,
          lowNormal: -Infinity,
          highNormal: Infinity,
        },
      ],
      // '[{"highAgeRange": "30", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "365", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "1825", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "5110", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "Infinity", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}]'
    },
  );

  const [currentStep, setCurrentStep] = useState(5);
  const [ageRangeFields, setAgeRangeFields] = useState([0]);

  const [formData, setFormData] = useState({
    testNameEnglish: "",
    testNameFrench: "",
    testReportNameEnglish: "",
    testReportNameFrench: "",
    testSection: "",
    panels: [],
    uom: "",
    loinc: "",
    resultType: "",
    orderable: "",
    notifyResults: "",
    inLabOnly: "",
    antimicrobialResistance: "",
    active: "",
    dictionary: [],
    dictionaryReference: "",
    defaultTestResult: "",
    sampleTypes: [],
    lowValid: "",
    highValid: "",
    lowReportingRange: "",
    highReportingRange: "",
    lowCritical: "",
    highCritical: "",
    significantDigits: "",
    resultLimits:
      [] ||
      '[{"highAgeRange": "30", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "365", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "1825", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "5110", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "Infinity", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}]',
  });

  const [showGuide, setShowGuide] = useState(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const handleNextStep = (newData, final = false) => {
    setFormData((prev) => ({ ...prev, ...newData }));

    if (!final) {
      handleTestAddPostCall(formData);
    } else {
      setCurrentStep((prevStep) => prevStep + 1);
    }
  };

  const handlePreviousStep = (newData) => {
    setFormData((prev) => ({ ...prev, ...newData }));
    setCurrentStep((prevStep) => prevStep - 1);
  };

  const validationSchema = Yup.object({
    testSection: Yup.string()
      .required("Test section is required")
      .notOneOf(["0"], "Please select a valid test section"),
    testNameEnglish: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("English test name is required"),
    testNameFrench: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("French test name is required"),
    testReportNameEnglish: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("English report name is required"),
    testReportNameFrench: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("French report name is required"),
    panels: Yup.array().of(
      Yup.object().shape({
        id: Yup.string().required("Panel ID is required"),
      }),
    ),
    uom: Yup.string().required("Unit of Measurement is required"),
    loinc: Yup.string()
      .matches(/^(?!-)(?:\d+-)*\d*$/, "Loinc must be a valid format")
      .required("Loinc is required"),
    resultType: Yup.string().required("Result Type is required"),
    orderable: Yup.string().oneOf(["Y", "N"], "Orderable must be Y or N"),
    notifyResults: Yup.string().oneOf(
      ["Y", "N"],
      "Notify Results must be Y or N",
    ),
    inLabOnly: Yup.string().oneOf(["Y", "N"], "In Lab Only must be Y or N"),
    antimicrobialResistance: Yup.string().oneOf(
      ["Y", "N"],
      "Antimicrobial Resistance must be Y or N",
    ),
    active: Yup.string().oneOf(["Y", "N"], "Active must be Y or N"),
    sampleTypes: Yup.array().of(
      Yup.object().shape({
        typeId: Yup.string().required("Sample Type ID is required"),
        tests: Yup.array()
          .of(
            Yup.object().shape({
              id: Yup.number().required("Test ID is required"),
            }),
          )
          .required("Tests are required"),
      }),
    ),
    lowValid: Yup.string().required("Low Valid is required"),
    highValid: Yup.string().required("High Valid is required"),
    lowReportingRange: Yup.string().required("Low Reporting Range is required"),
    highReportingRange: Yup.string().required(
      "High Reporting Range is required",
    ),
    lowCritical: Yup.string().required("Low Critical is required"),
    highCritical: Yup.string().required("High Critical is required"),
    significantDigits: Yup.string().required("Significant Digits is required"),
    resultLimits: Yup.string().required("Result Limits are required"),
  });

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestAdd`, (res) => {
      handleTestAddData(res);
    });
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  const handleTestAddData = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestAdd(res);
    }
  };

  useEffect(() => {
    if (testAdd) {
      setLabUnitList(testAdd.labUnitList || []);
      setPanelList(testAdd.panelList || []);
      setUomList(testAdd.uomList || []);
      setResultTypeList(testAdd.resultTypeList || []);
      setSampleTypeList(testAdd.sampleTypeList || []);
      setGroupedDictionaryList(testAdd.groupedDictionaryList || []);
      setDictionaryList(testAdd.dictionaryList || []);
      setAgeRangeList(testAdd.ageRangeList || []);
    }
  }, [testAdd]);

  useEffect(() => {
    if (selectedSampleType.length === 0) return;

    const fetchSampleTypeData = async (id) => {
      return new Promise((resolve, reject) => {
        try {
          getFromOpenElisServer(
            `/rest/sample-type-tests?sampleType=${id}`,
            (res) => {
              if (res) {
                handleSampleType(res);
                resolve(res);
              } else {
                reject(new Error("No response received"));
              }
            },
          );
        } catch (error) {
          console.error(`Error fetching data for sample type ${id}:`, error);
          reject(error);
        }
      });
    };

    const fetchAllSampleTypesData = async () => {
      try {
        await Promise.all(
          selectedSampleType.map((sampleType) =>
            fetchSampleTypeData(sampleType.id),
          ),
        );
      } catch (error) {
        console.error("Error fetching all sample types:", error);
      }
    };

    fetchAllSampleTypesData();
  }, [selectedSampleType]);

  const handleSampleType = (res) => {
    setSelectedSampleTypeResp((prev) => {
      const selectedSampleTypeIds = selectedSampleType.map((type) => type.id);

      const isInSelectedSampleType = selectedSampleTypeIds.includes(
        res.sampleTypeId,
      );

      if (isInSelectedSampleType) {
        const isAlreadyPresent = prev.some(
          (item) => item.sampleTypeId === res.sampleTypeId,
        );
        if (!isAlreadyPresent) {
          return [...prev, res];
        }
      } else {
        return prev.filter((item) => item.sampleTypeId !== res.sampleTypeId);
      }
      return prev;
    });
  };

  // const handleSampleTypeListSelectIdTestTag = (e) => {
  //   const selectedTestId = e.target.value;
  //   const testName = e.target.options[e.target.selectedIndex].text;

  //   const existingIndex = sampleTestTypeToGetTagList.findIndex(
  //     (item) => item.id === selectedTestId,
  //   );

  //   let updatedList;
  //   if (existingIndex !== -1) {
  //     updatedList = [...sampleTestTypeToGetTagList];
  //     updatedList.splice(existingIndex, 1);
  //     setSampleTestTypeToGetTagList(updatedList);
  //   } else {
  //     const selectedTest = {
  //       id: selectedTestId,
  //       name: testName,
  //     };
  //     updatedList = [...sampleTestTypeToGetTagList, selectedTest];
  //     setSampleTestTypeToGetTagList(updatedList);
  //   }

  //   const updatedReplace = updatedList.map((item) => item.id);
  //   setJsonWad((prevJsonWad) => ({
  //     ...prevJsonWad,
  //     replace: updatedReplace,
  //   }));
  // };

  const handleRemoveSampleTypeListSelectIdTestTag = (indexToRemove) => {
    setSampleTestTypeToGetTagList((prevTags) => {
      const updatedTags = prevTags.filter(
        (_, index) => index !== indexToRemove,
      );

      const updatedReplace = updatedTags.map((item) => item.id);
      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        replace: updatedReplace,
      }));

      return updatedTags;
    });

    setSelectedSampleTypeList((prevList) => {
      const updatedList = prevList.filter(
        (_, index) => index !== indexToRemove,
      );
      return updatedList;
    });

    setSelectedSampleType((prevList) => {
      const updatedList = prevList.filter(
        (_, index) => index !== indexToRemove,
      );
      return updatedList;
    });

    setSelectedSampleTypeResp((prevState) =>
      prevState.filter((_, index) => index !== indexToRemove),
    );
  };

  const handleTestAddPostCall = (values) => {
    if (!values) {
      window.location.reload();
      return;
    }
    console.log(values);
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestAdd`,
      JSON.stringify(values),
      (res) => {
        handelTestAddPostCallback(res);
      },
    );
  };

  const handelTestAddPostCallback = (res) => {
    if (res) {
      setIsLoading(false);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
  };

  const steps = [
    <StepOneTestNameAndTestSection
      key="step-1"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      labUnitList={labUnitList}
      setLabUnitList={setLabUnitList}
      selectedLabUnitId={selectedLabUnitList}
      setSelectedLabUnitList={setSelectedLabUnitList}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
    />,
    <StepTwoTestPanelAndUom
      key="step-2"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      panelList={panelList}
      setPanelList={setPanelList}
      uomList={uomList}
      setUomList={setUomList}
      panelListTag={panelListTag}
      setPanelListTag={setPanelListTag}
      selectedUomList={selectedUomList}
      setSelectedUomList={setSelectedUomList}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
    />,
    <StepThreeTestResultTypeAndLoinc
      key="step-3"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      resultTypeList={resultTypeList}
      setResultTypeList={setResultTypeList}
      selectedResultTypeList={selectedResultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
      intl={intl}
      addNotification={addNotification}
      setNotificationVisible={setNotificationVisible}
      lonic={lonic}
      setLonic={setLonic}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
    />,
    <StepFourSelectSampleTypeAndTestDisplayOrder
      key="step-4"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      sampleTypeList={sampleTypeList}
      setSampleTypeList={setSampleTypeList}
      selectedSampleTypeList={selectedSampleTypeList}
      setSelectedSampleTypeList={setSelectedSampleTypeList}
      sampleTestTypeToGetTagList={sampleTestTypeToGetTagList}
      setSampleTestTypeToGetTagList={setSampleTestTypeToGetTagList}
      selectedSampleType={selectedSampleType}
      setSelectedSampleType={setSelectedSampleType}
      selectedSampleTypeResp={selectedSampleTypeResp}
      setSelectedSampleTypeResp={setSelectedSampleTypeResp}
      handleRemoveSampleTypeListSelectIdTestTag={
        handleRemoveSampleTypeListSelectIdTestTag
      }
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      currentStep={currentStep}
    />,
    <StepFiveSelectListOptionsAndResultOrder
      key="step-5"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      groupedDictionaryList={groupedDictionaryList}
      setGroupedDictionaryList={setGroupedDictionaryList}
      selectedGroupedDictionaryList={selectedGroupedDictionaryList}
      setSelectedGroupedDictionaryList={setSelectedGroupedDictionaryList}
      dictionaryList={dictionaryList}
      setDictionaryList={setDictionaryList}
      dictionaryListTag={dictionaryListTag}
      setDictionaryListTag={setDictionaryListTag}
      singleSelectDictionaryList={singleSelectDictionaryList}
      setSingleSelectDictionaryList={setSingleSelectDictionaryList}
      multiSelectDictionaryList={multiSelectDictionaryList}
      setMultiSelectDictionaryList={setMultiSelectDictionaryList}
      multiSelectDictionaryListTag={multiSelectDictionaryListTag}
      setMultiSelectDictionaryListTag={setMultiSelectDictionaryListTag}
      handleRemoveSampleTypeListSelectIdTestTag={
        handleRemoveSampleTypeListSelectIdTestTag
      }
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      currentStep={currentStep}
    />,
    <StepSixSelectRangeAgeRangeAndSignificantDigits
      key="step-6"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      ageRangeList={ageRangeList}
      setAgeRangeList={setAgeRangeList}
      gotSelectedAgeRangeList={gotSelectedAgeRangeList}
      setGotSelectedAgeRangeList={setGotSelectedAgeRangeList}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      currentStep={currentStep}
      ageRangeFields={ageRangeFields}
      setAgeRangeFields={setAgeRangeFields}
    />,
    <StepSevenFinalDisplayAndSaveConfirmation
      key="step-7"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      panelListTag={panelListTag}
      setPanelListTag={setPanelListTag}
      selectedUomList={selectedUomList}
      setSelectedUomList={setSelectedUomList}
      selectedResultTypeList={selectedResultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
      selectedSampleTypeList={selectedSampleTypeList}
      setSelectedSampleTypeList={setSelectedSampleTypeList}
      sampleTestTypeToGetTagList={sampleTestTypeToGetTagList}
      setSampleTestTypeToGetTagList={setSampleTestTypeToGetTagList}
      selectedSampleType={selectedSampleType}
      setSelectedSampleType={setSelectedSampleType}
      selectedSampleTypeResp={selectedSampleTypeResp}
      setSelectedSampleTypeResp={setSelectedSampleTypeResp}
      dictionaryListTag={dictionaryListTag}
      setDictionaryListTag={setDictionaryListTag}
      singleSelectDictionaryList={singleSelectDictionaryList}
      setSingleSelectDictionaryList={setSingleSelectDictionaryList}
      multiSelectDictionaryList={multiSelectDictionaryList}
      setMultiSelectDictionaryList={setMultiSelectDictionaryList}
      multiSelectDictionaryListTag={multiSelectDictionaryListTag}
      setMultiSelectDictionaryListTag={setMultiSelectDictionaryListTag}
      ageRangeList={ageRangeList}
      setAgeRangeList={setAgeRangeList}
      gotSelectedAgeRangeList={gotSelectedAgeRangeList}
      setGotSelectedAgeRangeList={setGotSelectedAgeRangeList}
      currentStep={currentStep}
    />,
  ];

  const rows = [
    {
      id: "name",
      field: intl.formatMessage({ id: "field.name" }),
      description: <FormattedMessage id="test.description.name" />,
    },
    {
      id: "reportName",
      field: intl.formatMessage({ id: "field.reportName" }),
      description: <FormattedMessage id="test.description.reportName" />,
    },
    {
      id: "testSection",
      field: intl.formatMessage({ id: "field.testSection" }),
      description: <FormattedMessage id="test.description.testSection" />,
    },
    {
      id: "panel",
      field: intl.formatMessage({ id: "field.panel" }),
      description: <FormattedMessage id="test.description.panel" />,
    },
    {
      id: "uom",
      field: intl.formatMessage({ id: "field.uom" }),
      description: <FormattedMessage id="test.description.uom" />,
    },
    {
      id: "resultType",
      field: intl.formatMessage({ id: "field.resultType" }),
      description: (
        <>
          <p>
            <FormattedMessage id="description.resultType.kind" />
          </p>
          <ul>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.numeric" />
              </strong>
              <FormattedMessage id="description.resultType.numericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.alphanumeric" />
              </strong>
              <FormattedMessage id="description.resultType.alphanumericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.textArea" />
              </strong>
              <FormattedMessage id="description.resultType.textAreaDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.selectList" />
              </strong>
              <FormattedMessage id="description.resultType.selectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.multiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.multiSelectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.cascadingMultiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.cascadingMultiSelectListDesc" />
            </li>
          </ul>
        </>
      ),
    },
    {
      id: "active",
      field: intl.formatMessage({ id: "test.field.active" }),
      description: <FormattedMessage id="test.description.active" />,
    },
    {
      id: "orderable",
      field: intl.formatMessage({ id: "test.field.orderable" }),
      description: <FormattedMessage id="test.description.orderable" />,
    },
  ];

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  console.log(formData);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.test.add" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Toggle
                id="toggle"
                labelText="Show Guide"
                onClick={handleToggleShowGuide}
              />
            </Column>
          </Grid>
          {showGuide && <CustomShowGuide rows={rows} />}
          <br />
          <hr />
          <br />
          <div>{steps[currentStep]}</div>
          <br />
          <button
            onClick={() => {
              console.log(testAdd);
            }}
          >
            testAdd
          </button>
          <button
            onClick={() => {
              console.log(jsonWad);
            }}
          >
            jsonWad
          </button>
          <button
            onClick={() => {
              console.log(sampleTestTypeToGetTagList);
            }}
          >
            sampleTestTypeToGetTagList
          </button>
          <button
            onClick={() => {
              console.log(selectedSampleType);
            }}
          >
            selectedSampleType
          </button>
          <button
            onClick={() => {
              console.log(selectedSampleTypeResp);
            }}
          >
            selectedSampleTypeResp
          </button>
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestAdd);

const StepOneTestNameAndTestSection = ({
  formData,
  setFormData,
  validationSchema,
  handleNextStep,
  labUnitList,
  setLabUnitList,
  selectedLabUnitId,
  setSelectedLabUnitList,
  jsonWad,
  setJsonWad,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      <Formik
        initialValues={formData}
        enableReinitialize={true}
        validationSchema={Yup.object({
          testSection: Yup.string()
            .required("Test section is required")
            .notOneOf(["0"], "Please select a valid test section"),
          testNameEnglish: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("English test name is required"),
          testNameFrench: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("French test name is required"),
          testReportNameEnglish: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("English report name is required"),
          testReportNameFrench: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("French report name is required"),
        })}
        validateOnChange={true}
        validateOnBlur={true}
        onSubmit={(values, actions) => {
          handleSubmit(values);
          actions.setSubmitting(false);
        }}
      >
        {({
          values,
          handleChange,
          handleBlur,
          touched,
          errors,
          setFieldValue,
        }) => {
          const testNameEn = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testNameEnglish: e.target.value,
            }));
          };

          const testNameFr = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testNameFrench: e.target.value,
            }));
          };

          const reportingTestNameEn = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testReportNameEnglish: e.target.value,
            }));
          };

          const reportingTestNameFr = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testReportNameFrench: e.target.value,
            }));
          };

          const copyInputValuesFromTestNameEnFr = (values) => {
            setJsonWad((prev) => ({
              ...prev,
              testReportNameEnglish: values.testNameEnglish,
              testReportNameFrench: values.testNameFrench,
            }));
            setFieldValue("testReportNameEnglish", values.testNameEnglish);
            setFieldValue("testReportNameFrench", values.testNameFrench);
          };

          const handelTestSectionSelect = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testSection: e.target.value,
            }));

            setFieldValue("testSection", e.target.value);

            const selectedLabUnitObject = labUnitList.find(
              (item) => item.id === e.target.value,
            );

            if (selectedLabUnitObject) {
              setSelectedLabUnitList(selectedLabUnitObject);
            }
          };
          return (
            <Form>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <div>
                    <>
                      <FormattedMessage id="test.section.label" />
                      <span style={{ color: "red" }}>*</span>
                    </>
                    <br />
                    <Select
                      id={`select-test-section`}
                      hideLabel
                      required
                      name="testSection"
                      onChange={handelTestSectionSelect}
                      onBlur={handleBlur}
                      value={values.testSection}
                    >
                      <SelectItem value="0" text="Select Test Section" />
                      {labUnitList?.map((test) => (
                        <SelectItem
                          key={test.id}
                          value={test.id}
                          text={`${test.value}`}
                        />
                      ))}
                    </Select>
                  </div>
                  <br />
                  <div>
                    <>
                      <FormattedMessage id="sample.entry.project.testName" />
                      <span style={{ color: "red" }}>*</span>
                    </>
                    <br />
                    <br />
                    <FormattedMessage id="english.label" />
                    <br />
                    <TextInput
                      labelText=""
                      id="testNameEn"
                      name="testNameEnglish"
                      value={values.testNameEnglish}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testNameEnglish && !!errors.testNameEnglish
                      }
                      invalidText={
                        touched.testNameEnglish && errors.testNameEnglish
                      }
                    />
                    <br />
                    <FormattedMessage id="french.label" />
                    <br />
                    <TextInput
                      labelText=""
                      id="testNameFr"
                      name="testNameFrench"
                      value={values.testNameFrench}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testNameFrench && !!errors.testNameFrench
                      }
                      invalidText={
                        touched.testNameFrench && errors.testNameFrench
                      }
                    />
                  </div>
                  <br />
                  <div>
                    <>
                      <FormattedMessage id="reporting.label.testName" />
                      <span style={{ color: "red" }}>*</span>
                    </>
                    <br />
                    <br />
                    <Button
                      kind="tertiary"
                      onClick={() => {
                        copyInputValuesFromTestNameEnFr(values);
                      }}
                      type="button"
                    >
                      <FormattedMessage id="test.add.copy.name" />
                    </Button>
                    <br />
                    <br />
                    <FormattedMessage id="english.label" />
                    <br />
                    <TextInput
                      labelText=""
                      id="reportingTestNameEn"
                      name="testReportNameEnglish"
                      value={values.testReportNameEnglish}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testReportNameEnglish &&
                        !!errors.testReportNameEnglish
                      }
                      invalidText={
                        touched.testReportNameEnglish &&
                        errors.testReportNameEnglish
                      }
                    />
                    <br />
                    <FormattedMessage id="french.label" />
                    <br />
                    <TextInput
                      labelText=""
                      id="reportingTestNameFr"
                      name="testReportNameFrench"
                      value={values.testReportNameFrench}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testReportNameFrench &&
                        !!errors.testReportNameFrench
                      }
                      invalidText={
                        touched.testReportNameFrench &&
                        errors.testReportNameFrench
                      }
                    />
                  </div>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button type="submit">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => {
                      window.location.reload();
                    }}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="label.button.cancel" />
                  </Button>
                </Column>
              </Grid>
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

const StepTwoTestPanelAndUom = ({
  handleNextStep,
  handlePreviousStep,
  panelList,
  setPanelList,
  uomList,
  setUomList,
  panelListTag,
  setPanelListTag,
  formData,
  validationSchema,
  jsonWad,
  setJsonWad,
  selectedUomList,
  setSelectedUomList,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      <Formik
        initialValues={formData}
        validationSchema={Yup.object({
          panels: Yup.array()
            .min(1, "At least one panel must be selected")
            .of(
              Yup.object().shape({
                id: Yup.string()
                  .required("Panel ID is required")
                  .oneOf(
                    panelList.map((item) => item.id),
                    "Please select a valid panel",
                  ),
              }),
            ),
          uom: Yup.string()
            .min(1, "At least one unit of measurement must be selected")
            .oneOf(
              uomList.map((item) => String(item.id)),
              "Please select a valid unit of measurement",
            ),
        })}
        enableReinitialize={true}
        validateOnChange={true}
        validateOnBlur={true}
        onSubmit={(values, actions) => {
          handleSubmit(values);
          actions.setSubmitting(false);
        }}
      >
        {({
          values,
          handleChange,
          handleBlur,
          touched,
          errors,
          setFieldValue,
        }) => {
          const handelPanelSelectSetTag = (e) => {
            const selectedId = e.target.value;
            const selectedValue = e.target.options[e.target.selectedIndex].text;

            setPanelListTag((prevTags) => {
              const isTagPresent = prevTags.some(
                (tag) => tag.id === selectedId,
              );
              if (isTagPresent) return prevTags;

              const newTag = { id: selectedId, value: selectedValue };
              const updatedTags = [...prevTags, newTag];

              const updatedPanels = [
                ...updatedTags.map((tag) => ({ id: tag.id })),
              ];
              setJsonWad((prevJsonWad) => ({
                ...prevJsonWad,
                panels: updatedPanels,
              }));

              return updatedTags;
            });

            // setFieldValue("panel", selectedId);

            setFieldValue("panels", [...values.panels, { id: selectedId }]);

            setJsonWad((prev) => ({ ...prev, panel: selectedId }));
            const selectedPanelObject = panelList.find(
              (item) => item.id === selectedId,
            );
            if (selectedPanelObject) {
              setPanelList((prev) => [...prev, selectedPanelObject]);
            }
          };

          const handlePanelRemoveTag = (idToRemove) => {
            setPanelListTag((prevTags) => {
              const updatedTags = prevTags.filter(
                (tag) => tag.id !== idToRemove,
              );

              const updatedPanels = updatedTags.map((tag) => ({ id: tag.id }));
              setJsonWad((prevJsonWad) => ({
                ...prevJsonWad,
                panels: updatedPanels,
              }));

              return updatedTags;
            });
          };

          const handelUomSelect = (e) => {
            setJsonWad((prev) => ({ ...prev, uom: e.target.value }));

            const selectedUomObject = uomList.find(
              (item) => item.id === e.target.value,
            );

            setFieldValue("uom", e.target.value);

            if (selectedUomObject) {
              setSelectedUomList(selectedUomObject);
            }
          };

          return (
            <Form>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <FormattedMessage id="field.panel" />
                  <Select
                    onBlur={handleBlur}
                    id={`select-panel`}
                    name="panel"
                    onChange={(e) => {
                      handelPanelSelectSetTag(e);
                    }}
                    hideLabel
                    required
                    invalid={touched.panel && !!errors.panel}
                    invalidText={touched.panel && errors.panel}
                  >
                    <SelectItem value="0" text="Select Panel" />
                    {panelList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                  <br />
                  {panelListTag && panelListTag.length ? (
                    <div
                      className={"select-panel"}
                      style={{ marginBottom: "1.188rem" }}
                    >
                      <>
                        {panelListTag.map((panel) => (
                          <Tag
                            filter
                            key={`panelTags_${panel.id}`}
                            onClose={() => handlePanelRemoveTag(panel.id)}
                            style={{ marginRight: "0.5rem" }}
                            type={"green"}
                          >
                            {panel.value}
                          </Tag>
                        ))}
                      </>
                    </div>
                  ) : (
                    <></>
                  )}
                  <br />
                  <FormattedMessage id="field.uom" />
                  <Select
                    onBlur={handleBlur}
                    onChange={(e) => {
                      handelUomSelect(e);
                    }}
                    id={`select-uom`}
                    name="uom"
                    hideLabel
                    required
                    invalid={touched.uom && !!errors.uom}
                    invalidText={touched.uom && errors.uom}
                  >
                    <SelectItem value="0" text="Select Unit Of Measurement" />
                    {uomList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button type="submit">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => handlePreviousStep(values)}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

const StepThreeTestResultTypeAndLoinc = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  resultTypeList,
  setResultTypeList,
  selectedResultTypeList,
  setSelectedResultTypeList,
  intl,
  addNotification,
  setNotificationVisible,
  lonic,
  setLonic,
  jsonWad,
  setJsonWad,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      <Formik
        initialValues={formData}
        validationSchema={Yup.object({
          resultType: Yup.string()
            .oneOf(
              resultTypeList.map((item) => item.id),
              "Please select a valid Result Type",
            )
            .required("Result Type is required"),
          loinc: Yup.string()
            .matches(/^(?!-)(?:\d+-)*\d*$/, "Loinc must contain only numbers")
            .required("Loinc is required"),
          orderable: Yup.string().oneOf(["Y", "N"], "Orderable must be Y or N"),
          notifyResults: Yup.string().oneOf(
            ["Y", "N"],
            "Notify Results must be Y or N",
          ),
          inLabOnly: Yup.string().oneOf(
            ["Y", "N"],
            "In Lab Only must be Y or N",
          ),
          antimicrobialResistance: Yup.string().oneOf(
            ["Y", "N"],
            "Antimicrobial Resistance must be Y or N",
          ),
          active: Yup.string().oneOf(["Y", "N"], "Active must be Y or N"),
        })}
        enableReinitialize={true}
        validateOnChange={true}
        validateOnBlur={true}
        onSubmit={(values, actions) => {
          handleSubmit(values);
          actions.setSubmitting(false);
        }}
      >
        {({
          values,
          handleChange,
          handleBlur,
          touched,
          errors,
          setFieldValue,
        }) => {
          const handelLonicChange = (e) => {
            const regex = /^(?!-)(?:\d+-)*\d*$/;

            const value = e.target.value;

            setFieldValue("loinc", value);

            if (regex.test(value)) {
              setLonic(value);
              setJsonWad((prev) => ({ ...prev, loinc: value }));
            } else {
              addNotification({
                title: intl.formatMessage({
                  id: "notification.title",
                }),
                message: intl.formatMessage({
                  id: "notification.user.post.save.success",
                }),
                kind: NotificationKinds.error,
              });
              setNotificationVisible(true);
            }
          };

          const handelResultType = (e) => {
            setJsonWad((prev) => ({ ...prev, resultType: e.target.value }));

            const selectedResultTypeObject = resultTypeList.find(
              (item) => item.id == e.target.value,
            );

            setFieldValue("resultType", e.target.value);

            if (selectedResultTypeObject) {
              setSelectedResultTypeList(selectedResultTypeObject);
            }
          };

          const handleAntimicrobialResistance = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              antimicrobialResistance: e.target.checked ? "Y" : "N",
            }));

            setFieldValue(
              "antimicrobialResistance",
              e.target.checked ? "Y" : "N",
            );
          };
          const handleIsActive = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              active: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("active", e.target.checked ? "Y" : "N");
          };
          const handleOrderable = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              orderable: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("orderable", e.target.checked ? "Y" : "N");
          };
          const handleNotifyPatientofResults = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              notifyResults: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("notifyResults", e.target.checked ? "Y" : "N");
          };
          const handleInLabOnly = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              inLabOnly: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("inLabOnly", e.target.checked ? "Y" : "N");
          };

          return (
            <Form>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <div>
                    <>
                      <FormattedMessage id="field.resultType" />
                      <span style={{ color: "red" }}>*</span>
                    </>
                    <br />
                    <Select
                      onBlur={handleBlur}
                      id={`select-result-type`}
                      hideLabel
                      required
                      onChange={(e) => {
                        handelResultType(e);
                      }}
                      value={values.resultType}
                      invalid={touched.resultType && !!errors.resultType}
                      invalidText={touched.resultType && errors.resultType}
                    >
                      <SelectItem value="0" text="Select Result Type" />
                      {resultTypeList?.map((test) => (
                        <SelectItem
                          key={test.id}
                          value={test.id}
                          text={`${test.value}`}
                        />
                      ))}
                    </Select>
                  </div>
                  <br />
                  <div>
                    <FormattedMessage id="label.loinc" />
                    <br />
                    <TextInput
                      labelText=""
                      required
                      id="loinc"
                      name="loinc"
                      value={values.loinc}
                      placeholder={`Example : 430-0, 43166-0, 43167-8`}
                      onChange={(e) => {
                        handelLonicChange(e);
                        handleChange(e);
                      }}
                      invalid={touched.loinc && !!errors.loinc}
                      invalidText={touched.loinc && errors.loinc}
                    />
                  </div>
                  <br />
                  <div>
                    <Checkbox
                      labelText={
                        <FormattedMessage id="test.antimicrobialResistance" />
                      }
                      id="antimicrobial-resistance"
                      onChange={handleAntimicrobialResistance}
                      checked={values?.antimicrobialResistance === "Y"}
                    />
                    <Checkbox
                      labelText={
                        <FormattedMessage id="dictionary.category.isActive" />
                      }
                      id="is-active"
                      onChange={handleIsActive}
                      checked={values?.active === "Y"}
                    />
                    <Checkbox
                      labelText={<FormattedMessage id="label.orderable" />}
                      id="orderable"
                      onChange={handleOrderable}
                      checked={values?.orderable === "Y"}
                    />
                    <Checkbox
                      labelText={<FormattedMessage id="test.notifyResults" />}
                      id="notify-patient-of-results"
                      onChange={handleNotifyPatientofResults}
                      checked={values?.notifyResults === "Y"}
                    />
                    <Checkbox
                      labelText={<FormattedMessage id="test.inLabOnly" />}
                      id="in-lab-only"
                      onChange={handleInLabOnly}
                      checked={values?.inLabOnly === "Y"}
                    />
                  </div>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button type="submit">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => handlePreviousStep(values)}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

const StepFourSelectSampleTypeAndTestDisplayOrder = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  sampleTypeList,
  setSampleTypeList,
  selectedSampleTypeList,
  setSelectedSampleTypeList,
  sampleTestTypeToGetTagList,
  setSampleTestTypeToGetTagList,
  selectedSampleType,
  setSelectedSampleType,
  selectedSampleTypeResp,
  handleRemoveSampleTypeListSelectIdTestTag,
  setSelectedSampleTypeResp,
  jsonWad,
  setJsonWad,
  currentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      {currentStep === 3 ? (
        <>
          <Formik
            initialValues={formData}
            validationSchema={Yup.object({
              sampleTypes: Yup.array()
                .min(1, "At least one sample type must be selected")
                .of(
                  Yup.object().shape({
                    id: Yup.string().required("Sample Type ID is required"),
                    value: Yup.string().required(
                      "Sample Type Value is required",
                    ),
                    // typeId: Yup.string().required("Sample Type ID is required"),
                    // tests: Yup.string().required(
                    //   "Sample Type Value is required",
                    // ),
                  }),
                ),
            })}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              handleSubmit(values);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              const handleSampleTypeListSelectIdTestTag = (e) => {
                const selectedId = e.target.value;
                const selectedSampleTypeObject = sampleTypeList.find(
                  (type) => type.id === selectedId,
                );

                if (!selectedSampleTypeObject) return;

                const isAlreadySelected = selectedSampleType.some(
                  (type) => type.id === selectedSampleTypeObject.id,
                );

                if (!isAlreadySelected) {
                  const updatedList = [
                    ...selectedSampleTypeList,
                    selectedSampleTypeObject,
                  ];

                  setSelectedSampleTypeList(updatedList);
                  setFieldValue("sampleTypes", updatedList);
                  setSampleTestTypeToGetTagList((prev) => [
                    ...prev,
                    selectedSampleTypeObject,
                  ]);
                  setSelectedSampleType((prev) => [
                    ...prev,
                    selectedSampleTypeObject,
                  ]);
                }
              };

              const handleRemoveSampleTypeListSelectIdTestTag = (
                indexToRemove,
              ) => {
                setFieldValue("sampleTypes", [
                  ...selectedSampleTypeList.filter(
                    (_, index) => index !== indexToRemove,
                  ),
                ]);

                setSampleTestTypeToGetTagList((prevTags) => {
                  const updatedTags = prevTags.filter(
                    (_, index) => index !== indexToRemove,
                  );

                  const updatedReplace = updatedTags.map((item) => item.id);
                  setJsonWad((prevJsonWad) => ({
                    ...prevJsonWad,
                    replace: updatedReplace,
                  }));

                  return updatedTags;
                });

                setSelectedSampleTypeList((prevList) => {
                  const updatedList = prevList.filter(
                    (_, index) => index !== indexToRemove,
                  );
                  return updatedList;
                });

                setSelectedSampleType((prevList) => {
                  const updatedList = prevList.filter(
                    (_, index) => index !== indexToRemove,
                  );
                  return updatedList;
                });

                setSelectedSampleTypeResp((prevState) =>
                  prevState.filter((_, index) => index !== indexToRemove),
                );
              };

              console.log("Selected Sample Type Resp:", values.sampleTypes);

              return (
                <Form>
                  <Grid fullWidth={true}>
                    <Column lg={6} md={2} sm={4}>
                      <FormattedMessage id="sample.type" />
                      <br />
                      <Select
                        onBlur={handleBlur}
                        id={`select-sample-type`}
                        name="sampleTypes"
                        hideLabel
                        required
                        onChange={(e) => handleSampleTypeListSelectIdTestTag(e)}
                        invalid={touched.sampleTypes && !!errors.sampleTypes}
                        invalidText={touched.sampleTypes && errors.sampleTypes}
                      >
                        <SelectItem value="0" text="Select Sample Type" />
                        {sampleTypeList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      <br />
                      {sampleTestTypeToGetTagList &&
                      sampleTestTypeToGetTagList.length ? (
                        <div
                          className={"select-sample-type"}
                          style={{ marginBottom: "1.188rem" }}
                        >
                          <>
                            {sampleTestTypeToGetTagList.map(
                              (section, index) => (
                                <Tag
                                  filter
                                  key={`testTags_${index}`}
                                  onClose={() =>
                                    handleRemoveSampleTypeListSelectIdTestTag(
                                      index,
                                    )
                                  }
                                  style={{ marginRight: "0.5rem" }}
                                  type={"green"}
                                >
                                  {section.value}
                                </Tag>
                              ),
                            )}
                          </>
                        </div>
                      ) : (
                        <></>
                      )}
                      <br />
                    </Column>
                    <Column lg={10} md={6} sm={4}>
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="label.test.display.order" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                      <br />
                      {selectedSampleTypeResp.length > 0 ? (
                        selectedSampleTypeResp.map((item, index) => (
                          <>
                            <div className="gridBoundary">
                              {/* <Section key={index}>
                                <UnorderedList>
                                  {item.tests.map((test) => (
                                    <ListItem key={test.id}>
                                      {test.name}
                                    </ListItem>
                                  ))}
                                </UnorderedList>
                              </Section> */}
                              <Section key={index}>
                                <CustomCommonSortableOrderList
                                  test={item.tests}
                                  onSort={(updatedList) => {
                                    // console.log("Updated List:", updatedList);
                                  }}
                                  disableSorting={false}
                                />
                              </Section>
                            </div>
                            <br />
                          </>
                        ))
                      ) : (
                        <></>
                      )}
                    </Column>
                  </Grid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="next.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <></>
      )}
    </>
  );
};

const StepFiveSelectListOptionsAndResultOrder = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  groupedDictionaryList,
  setGroupedDictionaryList,
  selectedGroupedDictionaryList,
  setSelectedGroupedDictionaryList,
  dictionaryList,
  setDictionaryList,
  dictionaryListTag,
  setDictionaryListTag,
  singleSelectDictionaryList,
  setSingleSelectDictionaryList,
  multiSelectDictionaryList,
  setMultiSelectDictionaryList,
  multiSelectDictionaryListTag,
  setMultiSelectDictionaryListTag,
  handleRemoveSampleTypeListSelectIdTestTag,
  jsonWad,
  setJsonWad,
  currentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };
  return (
    <>
      {currentStep === 4 ? (
        <>
          <Formik
            initialValues={formData}
            // validationSchema={Yup.object({
            //   dictionary: Yup.array()
            //     .min(1, "At least one dictionary option must be selected")
            //     .of(
            //       Yup.object().shape({
            //         value: Yup.string()
            //           .required("Dictionary ID is required")
            //           .oneOf(
            //             dictionaryList.map((item) => item.id),
            //             "Please select a valid dictionary option",
            //           ),
            //         qualified: Yup.string().oneOf(
            //           ["Y", "N"],
            //           "Qualified must be Y or N",
            //         ),
            //       }),
            //     ),
            //   dictionaryReference: Yup.string().required(
            //     "Dictionary Reference is required",
            //   ),
            //   dictionaryDefault: Yup.string().required(
            //     "Dictionary Default is required",
            //   ),
            // })}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              handleSubmit(values);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              // const handleLabUnitSelect = (e) => {
              //   const selectedLabUnitId = e.target.value;

              //   setJsonWad((prev) => ({
              //     ...prev,
              //     testSection: selectedLabUnitId,
              //   }));
              // };

              const handelSelectListOptions = (e) => {
                const selectedId = e.target.value;

                const selectedObject = dictionaryList.find(
                  (item) => item.id === selectedId,
                );

                if (selectedObject) {
                  setSingleSelectDictionaryList((prev) => [
                    ...prev,
                    selectedObject,
                  ]);
                  setMultiSelectDictionaryList((prev) => [
                    ...prev,
                    selectedObject,
                  ]);

                  setDictionaryListTag((prev) => [...prev, selectedObject]);
                }

                //set the data object in jsonWad
              };

              const handleSelectQualifiersTag = (e) => {
                const selectedId = e.target.value;

                const selectedObject = multiSelectDictionaryList.find(
                  (item) => item.id === selectedId,
                );

                if (selectedObject) {
                  setMultiSelectDictionaryListTag((prev) => [
                    ...prev,
                    selectedObject,
                  ]);
                }

                //set the data object in jsonWad
              };

              return (
                <Form>
                  <Grid>
                    <Column lg={8} md={8} sm={4}>
                      <FormattedMessage id="label.select.list.options" />
                      {/* map the Select list options */}
                      <br />
                      <Select
                        onBlur={handleBlur}
                        id={`select-list-options`}
                        name="dictionary"
                        hideLabel
                        required
                        onChange={(e) => handelSelectListOptions(e)} // need a fix
                      >
                        <SelectItem value="0" text="Select List Option" />
                        {dictionaryList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      {/* tags need to display */}
                      <br />
                      {/* need to add tags */}
                      {dictionaryListTag && dictionaryListTag.length ? (
                        <div
                          className={"select-list-options-tag"}
                          style={{ marginBottom: "1.188rem" }}
                        >
                          <>
                            {dictionaryListTag.map((dict, index) => (
                              <Tag
                                filter
                                key={`list-options_${index}`}
                                // onClose={() =>
                                //   handleRemoveSampleTypeListSelectIdTestTag(index)
                                // }
                                style={{ marginRight: "0.5rem" }}
                                type={"green"}
                              >
                                {dict.value}
                              </Tag>
                            ))}
                          </>
                        </div>
                      ) : (
                        <></>
                      )}
                      <br />
                    </Column>
                    <Column lg={8} md={8} sm={4}>
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="label.result.order" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                      {/* remeder dragable & Select list options */}
                      {multiSelectDictionaryList &&
                        multiSelectDictionaryList?.length && (
                          <CustomCommonSortableOrderList
                            test={multiSelectDictionaryList}
                            disableSorting={false}
                            onSort={(updatedList) => {
                              // console.log(updatedList);
                            }}
                          />
                        )}
                      <br />
                      <br />
                      <FormattedMessage id="label.reference.value" />
                      <br />
                      {/* single Select */}
                      <Select
                        onBlur={handleBlur}
                        id={`select-reference-value`}
                        name="dictionaryReference"
                        hideLabel
                        required
                        // onChange={(e) => handleSampleTypeListSelectIdTestTag(e)} // need to fix
                      >
                        <SelectItem value="0" text="Select Reference Value" />
                        {singleSelectDictionaryList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      <br />
                      <br />
                      <FormattedMessage id="label.default.result" />
                      <br />
                      {/* single Select */}
                      <Select
                        onBlur={handleBlur}
                        id={`select-default-result`}
                        name="dictionaryDefault"
                        hideLabel
                        required
                        // onChange={(e) => handleSampleTypeListSelectIdTestTag(e)} // need to fix
                      >
                        <SelectItem
                          value="0"
                          text="Select Single Dictionary List"
                        />
                        {singleSelectDictionaryList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      <br />
                      <br />
                      <FormattedMessage id="label.qualifiers" />
                      <br />
                      <Select
                        onBlur={handleBlur}
                        id={`select-qualifiers`}
                        hideLabel
                        required
                        onChange={(e) => handleSelectQualifiersTag(e)} // need to fix
                      >
                        <SelectItem
                          value="0"
                          text="Select Multi Dictionary List"
                        />
                        {multiSelectDictionaryList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      <br />
                      {/* need to add tags */}
                      {multiSelectDictionaryListTag &&
                      multiSelectDictionaryListTag.length ? (
                        <div
                          className={"select-qualifiers-tag"}
                          style={{ marginBottom: "1.188rem" }}
                        >
                          <>
                            {multiSelectDictionaryListTag.map((dict, index) => (
                              <Tag
                                filter
                                key={`qualifiers_${index}`}
                                // onClose={() =>
                                //   handleRemoveSampleTypeListSelectIdTestTag(index)
                                // }
                                style={{ marginRight: "0.5rem" }}
                                type={"green"}
                              >
                                {dict.value}
                              </Tag>
                            ))}
                          </>
                        </div>
                      ) : (
                        <></>
                      )}
                      <br />
                    </Column>
                  </Grid>
                  <br />
                  <hr />
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="label.existing.test.sets" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                    </Column>
                  </Grid>
                  <br />
                  <hr />
                  <br />
                  <Grid fullWidth={true}>
                    {groupedDictionaryList &&
                      groupedDictionaryList.map((gdl, index) => {
                        return (
                          <Column
                            style={{ margin: "2px" }}
                            key={`grouped-dictionary-list-${index}`}
                            lg={4}
                            md={4}
                            sm={4}
                          >
                            <ClickableTile
                              onClick={() => {
                                // setSelectedGroupedDictionaryList([
                                //   ...selectedGroupedDictionaryList,
                                //   innerArray,
                                // ]);
                              }}
                            >
                              <Section>
                                <Section>
                                  <Section>
                                    <Section>
                                      <Heading
                                        style={{
                                          textDecoration: "underline",
                                        }}
                                      >
                                        Select
                                      </Heading>
                                    </Section>
                                  </Section>
                                </Section>
                              </Section>
                              {gdl &&
                                gdl.map((gdlVal) => {
                                  return (
                                    <div key={gdlVal.id}>{gdlVal.value}</div>
                                  );
                                })}
                            </ClickableTile>
                          </Column>
                        );
                      })}
                  </Grid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="next.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <></>
      )}
    </>
  );
};

const StepSixSelectRangeAgeRangeAndSignificantDigits = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  ageRangeList,
  setAgeRangeList,
  gotSelectedAgeRangeList,
  setGotSelectedAgeRangeList,
  jsonWad,
  setJsonWad,
  currentStep,
  ageRangeFields,
  setAgeRangeFields,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };
  return (
    <>
      {currentStep === 5 ? (
        <>
          <Formik
            initialValues={formData}
            // validationSchema={validationSchema}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              handleSubmit(values);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              const handleAddAgeRangeFillUp = () => {
                setAgeRangeFields((prev) => [...prev, prev.length]);
              };

              const handleRemoveAgeRangeFillUp = (indexToRemove) => {
                setAgeRangeFields((prev) =>
                  prev.filter((_, i) => i !== indexToRemove),
                );
              };

              return (
                <Form>
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="label.button.range" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                    </Column>
                  </Grid>
                  <br />
                  <hr />
                  <br />
                  <Grid fullWidth={true} className="gridBoundary">
                    <Column lg={16} md={8} sm={4}>
                      <FormattedMessage id="field.ageRange" />
                      <hr />
                    </Column>
                    {ageRangeFields.map((_, index) => (
                      <React.Fragment key={index}>
                        <Column
                          key={index}
                          lg={4}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <Checkbox
                            id={`gender-${index}`}
                            name={`gender-${index}`}
                            disabled={true}
                            labelText={
                              <FormattedMessage id="label.sex.dependent" />
                            }
                            // onChange={() => {}}
                          />
                          {/* render male & female on checkbox*/}
                          {/* { ? 'Male/Female' : ''} */}
                        </Column>
                        <Column
                          key={index}
                          lg={4}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <RadioButtonGroup
                            id={`fieldAgeRangeRadioGroup-${index}`}
                            name={`fieldAgeRangeRadioGroup-${index}`}
                          >
                            <RadioButton labelText={"Y"} />
                            <RadioButton labelText={"M"} />
                            <RadioButton labelText={"D"} />
                          </RadioButtonGroup>
                        </Column>
                        <Column
                          key={index}
                          lg={4}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <NumberInput
                            id={`ageRangeFields[${index}].ageRange0`}
                            name={`ageRangeFields[${index}].ageRange0`}
                            onBlur={handleBlur}
                            label="Age Range"
                            hideLabel
                            size={"md"}
                            min={0}
                            max={100} // dependent to selecting
                            required
                            step={1}
                            value={0}
                            // invalid={
                            //   touched?.ageRangeFields?.[index]?.ageRange0 &&
                            //   !!errors?.ageRangeFields?.[index]?.ageRange0
                            // }
                            invalidText={
                              touched?.ageRangeFields?.[index]?.ageRange0 &&
                              errors?.ageRangeFields?.[index]?.ageRange0
                            }
                          />
                        </Column>
                        <Column
                          key={index}
                          lg={4}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <Select
                            onBlur={handleBlur}
                            id={`ageRangeFields[${index}].ageRange1`}
                            name={`ageRangeFields[${index}].ageRange1`}
                            labelText=""
                            hideLabel
                            size={"md"}
                            min={0}
                            max={100} // dependent to selecting
                            required
                            step={1}
                            value={0}
                            // invalid={
                            //   touched?.ageRangeFields?.[index]?.ageRange1 &&
                            //   !!errors?.ageRangeFields?.[index]?.ageRange1
                            // }
                            invalidText={
                              touched?.ageRangeFields?.[index]?.ageRange1 &&
                              errors?.ageRangeFields?.[index]?.ageRange1
                            }
                          >
                            {/* map agerangeList values Form objects inside array */}
                            <SelectItem value={"0"} text={`Select Age Range`} />
                            {ageRangeList.map((age) => (
                              <SelectItem
                                key={age.id}
                                value={age.id}
                                text={`${age.value}`}
                              />
                            ))}
                          </Select>
                        </Column>
                        <Column
                          key={index}
                          lg={8}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <FormattedMessage id="field.normalRange" />
                          <hr />
                          <div style={{ display: "flex", gap: "4px" }}>
                            <NumberInput
                              id={`ageRangeFields[${index}].normalRange0`}
                              name={`ageRangeFields[${index}].normalRange0`}
                              onBlur={handleBlur}
                              label="Lower Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]
                              //     ?.normalRange0 &&
                              //   !!errors?.ageRangeFields?.[index]?.normalRange0
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]
                                  ?.normalRange0 &&
                                errors?.ageRangeFields?.[index]?.normalRange0
                              }
                            />
                            <NumberInput
                              id={`ageRangeFields[${index}].normalRange1`}
                              name={`ageRangeFields[${index}].normalRange1`}
                              onBlur={handleBlur}
                              label="Higher Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]
                              //     ?.normalRange1 &&
                              //   !!errors?.ageRangeFields?.[index]?.normalRange1
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]
                                  ?.normalRange1 &&
                                errors?.ageRangeFields?.[index]?.normalRange1
                              }
                            />
                            {/* render  two extra fields for TextInput on Click of Check box */}
                          </div>
                        </Column>
                        <Column
                          key={index}
                          lg={8}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <FormattedMessage id="label.reporting.range" />
                          <hr />
                          <div style={{ display: "flex", gap: "4px" }}>
                            <NumberInput
                              id={`ageRangeFields[${index}].reportingRange0`}
                              name={`ageRangeFields[${index}].reportingRange0`}
                              onBlur={handleBlur}
                              label="Lower Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]
                              //     ?.reportingRange0 &&
                              //   !!errors?.ageRangeFields?.[index]
                              //     ?.reportingRange0
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]
                                  ?.reportingRange0 &&
                                errors?.ageRangeFields?.[index]?.reportingRange0
                              }
                            />
                            <NumberInput
                              id={`ageRangeFields[${index}].reportingRange1`}
                              name={`ageRangeFields[${index}].reportingRange1`}
                              onBlur={handleBlur}
                              label="Higher Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]
                              //     ?.reportingRange1 &&
                              //   !!errors?.ageRangeFields?.[index]
                              //     ?.reportingRange1
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]
                                  ?.reportingRange1 &&
                                errors?.ageRangeFields?.[index]?.reportingRange1
                              }
                            />
                          </div>
                        </Column>
                        <Column
                          key={index}
                          lg={8}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <FormattedMessage id="field.validRange" />
                          <hr />
                          <div style={{ display: "flex", gap: "4px" }}>
                            <NumberInput
                              id={`ageRangeFields[${index}].validRange0`}
                              name={`ageRangeFields[${index}].validRange0`}
                              onBlur={handleBlur}
                              label="Lower Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]?.validRange0 &&
                              //   !!errors?.ageRangeFields?.[index]?.validRange0
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]?.validRange0 &&
                                errors?.ageRangeFields?.[index]?.validRange0
                              }
                            />
                            <NumberInput
                              id={`ageRangeFields[${index}].validRange1`}
                              name={`ageRangeFields[${index}].validRange1`}
                              onBlur={handleBlur}
                              label="Higher Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]?.validRange1 &&
                              //   !!errors?.ageRangeFields?.[index]?.validRange1
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]?.validRange1 &&
                                errors?.ageRangeFields?.[index]?.validRange1
                              }
                            />
                          </div>
                        </Column>
                        <Column
                          key={index}
                          lg={8}
                          md={4}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <FormattedMessage id="label.critical.range" />
                          <hr />
                          <div style={{ display: "flex", gap: "4px" }}>
                            <NumberInput
                              id={`ageRangeFields[${index}].criticalRange0`}
                              name={`ageRangeFields[${index}].criticalRange0`}
                              onBlur={handleBlur}
                              label="Lower Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]
                              //     ?.criticalRange0 &&
                              //   !!errors?.ageRangeFields?.[index]
                              //     ?.criticalRange0
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]
                                  ?.criticalRange0 &&
                                errors?.ageRangeFields?.[index]?.criticalRange0
                              }
                            />
                            <NumberInput
                              id={`ageRangeFields[${index}].criticalRange1`}
                              name={`ageRangeFields[${index}].criticalRange1`}
                              onBlur={handleBlur}
                              label="Higher Range"
                              size={"md"}
                              min={0}
                              max={100} // dependent to selecting
                              required
                              step={1}
                              value={0}
                              // invalid={
                              //   touched?.ageRangeFields?.[index]
                              //     ?.criticalRange1 &&
                              //   !!errors?.ageRangeFields?.[index]
                              //     ?.criticalRange1
                              // }
                              invalidText={
                                touched?.ageRangeFields?.[index]
                                  ?.criticalRange1 &&
                                errors?.ageRangeFields?.[index]?.criticalRange1
                              }
                            />
                          </div>
                        </Column>
                        <Column
                          key={index}
                          lg={16}
                          md={8}
                          sm={4}
                          style={{ marginTop: "1rem" }}
                        >
                          <div
                            key={`remove-age-range-fill-up-${index}`}
                            style={{
                              display: "flex",
                              justifyContent: "flex-end",
                            }}
                          >
                            <Button
                              id={`remove-age-range-fill-up-${index}`}
                              name={`remove-age-range-fill-up-${index}`}
                              kind="danger"
                              type="button"
                              onClick={() => handleRemoveAgeRangeFillUp(index)}
                            >
                              Remove
                            </Button>
                          </div>
                        </Column>
                      </React.Fragment>
                    ))}
                    <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
                      <Button
                        onClick={() => {
                          handleAddAgeRangeFillUp();
                        }}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="Add +" />
                      </Button>
                    </Column>
                  </Grid>
                  <br />
                  <FlexGrid fullWidth={true}>
                    <Row>
                      <Column lg={4} md={4} sm={4}>
                        <Section>
                          <Section>
                            <Section>
                              <Heading>
                                <FormattedMessage id="field.significantDigits" />
                                {" : "}
                              </Heading>
                            </Section>
                          </Section>
                        </Section>
                      </Column>
                      <Column lg={4} md={4} sm={4}>
                        <NumberInput
                          id={"significant_digits_num_input"}
                          max={99}
                          min={0}
                          size={"md"}
                          allowEmpty={true}
                          invalid={
                            touched.significantDigits &&
                            !!errors.significantDigits
                          }
                          invalidText={
                            touched.significantDigits &&
                            errors.significantDigits
                          }
                        />
                      </Column>
                    </Row>
                  </FlexGrid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="next.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <></>
      )}
    </>
  );
};

const StepSevenFinalDisplayAndSaveConfirmation = ({
  formData,
  validationSchema,
  handlePreviousStep,
  handleNextStep,
  jsonWad,
  setJsonWad,
  panelListTag,
  selectedLabUnitList,
  selectedUomList,
  selectedResultTypeList,
  selectedSampleTypeList,
  selectedSampleTypeResp,
  currentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, false);
  };
  return (
    <>
      {currentStep === 7 - 1 ? (
        <>
          <Formik
            initialValues={formData}
            // validationSchema={validationSchema}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              handleSubmit(values);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              return (
                <Form>
                  <Grid fullWidth={true}>
                    <Column lg={6} md={8} sm={4}>
                      <FormattedMessage id="sample.entry.project.testName" />
                      <br />
                      <FormattedMessage id="english.label" />
                      {" : "}
                      {values?.testNameEnglish}
                      <br />
                      <FormattedMessage id="french.label" />
                      {" : "}
                      {values?.testNameFrench}
                      <br />
                      <br />
                      <FormattedMessage id="reporting.label.testName" />
                      <br />
                      <FormattedMessage id="english.label" />
                      {" : "}
                      {values?.reportingTestNameEn}
                      <br />
                      <FormattedMessage id="french.label" />
                      {" : "}
                      {values?.reportingTestNameFr}
                      <br />
                      <br />
                      <FormattedMessage id="test.section.label" />
                      {" : "}
                      {selectedLabUnitList?.value}
                      <br />
                      <br />
                      <FormattedMessage id="field.panel" />
                      {" : "}
                      {/* map the  {panelList[0].value} in and there values in line*/}
                      {panelListTag.length > 0 ? (
                        <UnorderedList>
                          {panelListTag.map((tag) => (
                            <div key={tag.id} style={{ marginRight: "0.5rem" }}>
                              <ListItem>{tag.value}</ListItem>
                            </div>
                          ))}
                        </UnorderedList>
                      ) : (
                        <></>
                      )}
                      <br />
                      <br />
                      <FormattedMessage id="field.uom" />
                      {" : "}
                      {selectedUomList?.value}
                      <br />
                      <br />
                      <FormattedMessage id="label.loinc" />
                      {" : "}
                      {values?.loinc}
                      <br />
                      <br />
                      <FormattedMessage id="field.resultType" />
                      {" : "}
                      {selectedResultTypeList.value}
                      <br />
                      <br />
                      <FormattedMessage id="test.antimicrobialResistance" />
                      {" : "}
                      {values?.antimicrobialResistance}
                      <br />
                      <br />
                      <FormattedMessage id="dictionary.category.isActive" />
                      {" : "}
                      {values?.active}
                      <br />
                      <br />
                      <FormattedMessage id="label.orderable" />
                      {" : "}
                      {values?.orderable}
                      <br />
                      <br />
                      <FormattedMessage id="test.notifyResults" />
                      {" : "}
                      {values?.notifyResults}
                      <br />
                      <br />
                      <FormattedMessage id="test.inLabOnly" />
                      {" : "}
                      {values?.inLabOnly}
                      <br />
                    </Column>
                    <Column lg={10} md={8} sm={4}>
                      <FormattedMessage id="sample.type.and.test.sort.order" />
                      {/* Mapp the combbination of the selecte[sampleType] & tests of [sampleType] in sorted order */}
                      <br />
                      {selectedSampleTypeList.length > 0 ? (
                        <UnorderedList nested={true}>
                          {selectedSampleTypeList.map((type, index) => (
                            <div key={`selectedSampleType_${index}`}>
                              <ListItem>{type.value}</ListItem>
                              <br />
                              {selectedSampleTypeResp
                                .filter((resp) => resp.sampleTypeId === type.id)
                                .map((item, respIndex) => (
                                  <div
                                    key={`selectedSampleTypeResp_${respIndex}`}
                                    className="gridBoundary"
                                  >
                                    <Section>
                                      <UnorderedList nested>
                                        {item.tests.map((test) => (
                                          <ListItem key={`test_${test.id}`}>
                                            {test.name}
                                          </ListItem>
                                        ))}
                                      </UnorderedList>
                                    </Section>
                                  </div>
                                ))}
                            </div>
                          ))}
                        </UnorderedList>
                      ) : (
                        <></>
                      )}
                    </Column>
                  </Grid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="accept.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <></>
      )}
    </>
  );
};

// compitative selection
// validation schema flow fix for each step
// func moving to form ground
// fucntion buildup for 5-6-7 step
// better formating at the end of the step 7
