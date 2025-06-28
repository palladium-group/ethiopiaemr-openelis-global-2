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
import {
  StepFiveSelectListOptionsAndResultOrder,
  StepFourSelectSampleTypeAndTestDisplayOrder,
  StepOneTestNameAndTestSection,
  StepSevenFinalDisplayAndSaveConfirmation,
  StepSixSelectRangeAgeRangeAndSignificantDigits,
  StepThreeTestResultTypeAndLoinc,
  StepTwoTestPanelAndUom,
} from "./customComponents/TestStepForm.js";

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
  const [currentStep, setCurrentStep] = useState(0);
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
  });

  const [showGuide, setShowGuide] = useState(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const handleNextStep = (newData, final = false) => {
    setFormData((prev) => ({ ...prev, ...newData }));

    if (!final) {
      handleTestAddPostCall(formData);
    }

    const selectedResultTypeId = newData?.resultType || formData.resultType;

    setCurrentStep((prev) => {
      if (prev === 3) {
        if (["1", "5"].includes(selectedResultTypeId)) {
          return prev + 3;
        }

        if (["4"].includes(selectedResultTypeId)) {
          return prev + 2;
        }

        if (["2", "6", "7"].includes(selectedResultTypeId)) {
          return prev + 1;
        }
      }

      if (prev === 4 && ["2", "6", "7"].includes(selectedResultTypeId)) {
        return prev + 2;
      }

      if (prev === 5 && selectedResultTypeId === "4") {
        return prev + 1;
      }

      return prev + 1;
    });
  };

  const handlePreviousStep = (newData) => {
    setFormData((prev) => ({ ...prev, ...newData }));
    const selectedResultTypeId = newData?.resultType || formData.resultType;

    setCurrentStep((prevStep) => {
      if (prevStep === 6) {
        if (["1", "5"].includes(selectedResultTypeId)) {
          return prevStep - 3;
        }

        if (["2", "6", "7"].includes(selectedResultTypeId)) {
          return prevStep - 2;
        }

        if (selectedResultTypeId === "4") {
          return prevStep - 1;
        }
      }

      if (prevStep === 5 && selectedResultTypeId === "4") {
        return prevStep - 2;
      }

      return prevStep - 1;
    });
  };

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
    const selectedSampleTypeIds = selectedSampleType.map((type) => type.id);

    const isInSelectedSampleType = selectedSampleTypeIds.includes(
      res.sampleTypeId,
    );

    const extraTestItem = {
      id: "0",
      name: formData.testNameEnglish,
      userBenchChoice: false,
    };

    setSelectedSampleTypeResp((prev) => {
      const isAlreadyPresent = prev.some(
        (item) => item.sampleTypeId === res.sampleTypeId,
      );

      let updated;

      if (isInSelectedSampleType && !isAlreadyPresent) {
        updated = [
          ...prev,
          {
            ...res,
            tests: [...(res.tests || []), extraTestItem],
          },
        ];
      } else if (!isInSelectedSampleType) {
        updated = prev.filter((item) => item.sampleTypeId !== res.sampleTypeId);
      } else {
        updated = prev;
      }
      return updated;
    });
  };

  const handleTestAddPostCall = (values) => {
    if (!values) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Form submission failed due to missing data.",
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 500);
    }
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestAdd`,
      JSON.stringify({ jsonWad: JSON.stringify(values) }),
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
      handleNextStep={handleNextStep}
      labUnitList={labUnitList}
      setLabUnitList={setLabUnitList}
      selectedLabUnitId={selectedLabUnitList}
      setSelectedLabUnitList={setSelectedLabUnitList}
    />,
    <StepTwoTestPanelAndUom
      key="step-2"
      formData={formData}
      setFormData={setFormData}
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
    />,
    <StepThreeTestResultTypeAndLoinc
      key="step-3"
      formData={formData}
      setFormData={setFormData}
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
    />,
    <StepFourSelectSampleTypeAndTestDisplayOrder
      key="step-4"
      formData={formData}
      setFormData={setFormData}
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
      currentStep={currentStep}
    />,
    <StepFiveSelectListOptionsAndResultOrder
      key="step-5"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      groupedDictionaryList={groupedDictionaryList}
      setGroupedDictionaryList={setGroupedDictionaryList}
      selectedGroupedDictionaryList={selectedGroupedDictionaryList}
      setSelectedGroupedDictionaryList={setSelectedGroupedDictionaryList}
      dictionaryList={dictionaryList}
      setDictionaryList={setDictionaryList}
      dictionaryListTag={dictionaryListTag}
      resultTypeList={resultTypeList}
      setDictionaryListTag={setDictionaryListTag}
      selectedResultTypeList={selectedResultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
      singleSelectDictionaryList={singleSelectDictionaryList}
      setSingleSelectDictionaryList={setSingleSelectDictionaryList}
      multiSelectDictionaryList={multiSelectDictionaryList}
      setMultiSelectDictionaryList={setMultiSelectDictionaryList}
      multiSelectDictionaryListTag={multiSelectDictionaryListTag}
      setMultiSelectDictionaryListTag={setMultiSelectDictionaryListTag}
      currentStep={currentStep}
      setCurrentStep={setCurrentStep}
    />,
    <StepSixSelectRangeAgeRangeAndSignificantDigits
      key="step-6"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      selectedResultTypeList={selectedResultTypeList}
      ageRangeList={ageRangeList}
      setAgeRangeList={setAgeRangeList}
      gotSelectedAgeRangeList={gotSelectedAgeRangeList}
      setGotSelectedAgeRangeList={setGotSelectedAgeRangeList}
      currentStep={currentStep}
      setCurrentStep={setCurrentStep}
      ageRangeFields={ageRangeFields}
      setAgeRangeFields={setAgeRangeFields}
    />,
    <StepSevenFinalDisplayAndSaveConfirmation
      key="step-7"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      panelListTag={panelListTag}
      setPanelListTag={setPanelListTag}
      selectedUomList={selectedUomList}
      setSelectedUomList={setSelectedUomList}
      selectedLabUnitList={selectedLabUnitList}
      setSelectedLabUnitList={setSelectedLabUnitList}
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
          <hr />
          <br />
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestAdd);
