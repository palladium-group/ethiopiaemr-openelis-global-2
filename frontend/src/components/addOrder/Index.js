import React, { useContext, useEffect, useState } from "react";
import { Button, ProgressIndicator, ProgressStep, Stack } from "@carbon/react";
import PatientInfo from "./PatientInfo";
import AddSample from "./AddSample";
import AddOrder from "./AddOrder";
import "./add-order.scss";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import OrderEntryAdditionalQuestions from "./OrderEntryAdditionalQuestions";
import OrderSuccessMessage from "./OrderSuccessMessage";
import { FormattedMessage, useIntl } from "react-intl";
import OrderEntryValidationSchema from "../formModel/validationSchema/OrderEntryValidationSchema";
import config from "../../config.json";
import PageBreadCrumb from "../common/PageBreadCrumb";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.addorder", link: "/SamplePatientEntry" },
];

export let sampleObject = {
  index: 0,
  sampleRejected: false,
  rejectionReason: "",
  sampleTypeId: "",
  sampleXML: null,
  panels: [],
  tests: [],
  requestReferralEnabled: false,
  referralItems: [],
};
const Index = () => {
  const intl = useIntl();

  const firstPageNumber = 0;
  const lastPageNumber = 4;
  const patientInfoPageNumber = firstPageNumber;
  const programPageNumber = firstPageNumber + 1;
  const samplePageNumber = firstPageNumber + 2;
  const orderPageNumber = firstPageNumber + 3;
  const successMsgPageNumber = lastPageNumber;
  const [changed, setChanged] = useState({
    "sampleOrderItems.providerFirstName": false,
    "sampleOrderItems.providerLastName": false,
    "sampleOrderItems.labNo": false,
  });
  const [page, setPage] = useState(firstPageNumber);
  const [orderFormValues, setOrderFormValues] = useState(SampleOrderFormValues);
  const [samples, setSamples] = useState([sampleObject]);
  const [errors, setErrors] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [phoneValidation, setPhoneValidation] = useState({
    primaryPhone: { body: "", status: true },
    contactPhone: { body: "", status: true },
  });

  let SampleTypes = [];
  let sampleTypeMap = {};
  let CrossPanels = [];
  let CrossTests = [];
  let sampleTypeOrder;
  let crossSampleTypeMap = {};
  let crossSampleTypeOrderMap = {};

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);
  const urlParams = new URLSearchParams(window.location.search);
  const hasIncomingOrderInUrl = Boolean(
    urlParams.get("ID") || urlParams.get("IDs"),
  );
  const isIncomingOrderFlow =
    configurationProperties.ACCEPT_EXTERNAL_ORDERS === "true" &&
    (hasIncomingOrderInUrl ||
      Boolean(orderFormValues.sampleOrderItems.externalOrderNumber));

  useEffect(() => {
    if (configurationProperties.ACCEPT_EXTERNAL_ORDERS === "true") {
      const urlParams = new URLSearchParams(window.location.search);
      const externalIds = urlParams.get("IDs");
      const externalId = urlParams.get("ID");
      const groupedIds = externalIds
        ? externalIds
            .split(",")
            .map((id) => id.trim())
            .filter((id) => id.length > 0)
        : [];
      if (groupedIds.length > 0) {
        const primaryOrderId = groupedIds[0];
        setOrderFormValues((prev) => ({
          ...prev,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            externalOrderNumber: primaryOrderId,
            externalOrderNumbers: groupedIds.join(","),
          },
        }));
        loadGroupedOrders(groupedIds);
      } else if (externalId) {
        setOrderFormValues((prev) => ({
          ...prev,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            externalOrderNumber: externalId,
            externalOrderNumbers: externalId,
          },
        }));
        checkOrderReferral(externalId);
      }
    } else {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          externalOrderNumber: "",
          externalOrderNumbers: "",
        },
      });
    }
  }, [configurationProperties.ACCEPT_EXTERNAL_ORDERS]);

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const hasOrderInUrl = urlParams.get("ID") || urlParams.get("IDs");
    if (
      !hasOrderInUrl &&
      orderFormValues.sampleOrderItems.externalOrderNumber &&
      !orderFormValues.sampleOrderItems.externalOrderNumbers
    ) {
      checkOrderReferral(orderFormValues.sampleOrderItems.externalOrderNumber);
    }
  }, [orderFormValues.sampleOrderItems.externalOrderNumber]);

  const checkOrderReferral = (externalOrderNumber) => {
    if (externalOrderNumber) {
      getLabOrder(externalOrderNumber, (response) =>
        processLabOrderSuccess(
          response,
          false,
          {
            externalOrderNumber,
            externalOrderNumbers: externalOrderNumber,
          },
          true,
          externalOrderNumber,
        ),
      );
    }
  };

  const loadGroupedOrders = (orderNumbers, index = 0) => {
    if (!orderNumbers || index >= orderNumbers.length) {
      return;
    }
    const orderNumber = orderNumbers[index];
    getLabOrder(orderNumber, (response) => {
      processLabOrderSuccess(
        response,
        index > 0,
        {
          externalOrderNumber: orderNumbers[0],
          externalOrderNumbers: orderNumbers.join(","),
        },
        index === orderNumbers.length - 1,
        orderNumber,
      );
      loadGroupedOrders(orderNumbers, index + 1);
    });
  };

  const getLabOrder = (orderNumber, success, failure) => {
    if (!failure) {
      failure = () => {
        // Default failure handler - no-op
      };
    }

    fetch(
      config.serverBaseUrl +
        "/ajaxQueryXML?asJSON=true&provider=LabOrderSearchProvider&orderNumber=" +
        orderNumber,
      {
        method: "get",
        //indicator: 'throbbing',
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      },
    )
      .then((response) => response.json())
      .then((jsonResponse) => {
        success(jsonResponse);
      })
      .catch((error) => {
        console.error(error);
        if (error instanceof SyntaxError) {
          addNotification({
            title: intl.formatMessage({
              id: "notification.title",
            }),
            message: intl.formatMessage({
              id: "notification.response.syntax.error",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
        failure();
      });
  };

  const processLabOrderSuccess = (
    labOrder,
    keepExistingData = false,
    externalOrderInfo = {},
    shouldFinalizeState = true,
    serviceRequestIdForThisOrder = null,
  ) => {
    // clearOrderData();
    let message = labOrder.fieldmessage.message;
    let formField = labOrder.fieldmessage.formfield;
    let order = formField.order;

    let newOrderFormValues = { ...orderFormValues };

    if (!keepExistingData) {
      SampleTypes = [];
      CrossPanels = [];
      CrossTests = [];
      sampleTypeMap = {};
    }

    //TODO all these actions mimic other areas of the code. Possible rework could centralize these calls into a context
    if (message === "valid") {
      // PATIENT
      if (order.patient) {
        parsePatient(newOrderFormValues, order.patient);
      }

      // REQUESTER
      if (order.requester) {
        parseRequester(newOrderFormValues, order.requester);
      }

      if (order.requestingOrg) {
        parseRequestingOrg(newOrderFormValues, order.requestingOrg);
      }
      if (order.location && !order.requestingOrg.id) {
        parseLocation(newOrderFormValues, order.location);
      }

      if (order.user_alert) {
        alert(order.user_alert);
      }

      // initialize objects and globals
      sampleTypeOrder = -1;
      crossSampleTypeMap = {};
      crossSampleTypeOrderMap = {};

      if (order.sampleTypes != "") {
        parseSampletypes(
          newOrderFormValues,
          order.sampleTypes instanceof Array
            ? order.sampleTypes
            : [{ sampleType: order.sampleTypes.sampleType }],
          SampleTypes,
          serviceRequestIdForThisOrder,
        );
      }

      if (order.referringDiagnoses && order.referringDiagnoses != "") {
        parseReferringDiagnoses(newOrderFormValues, order.referringDiagnoses);
      }

      if (order.emergencyContact && order.emergencyContact != "") {
        parseEmergencyContact(newOrderFormValues, order.emergencyContact);
      }

      // One-page incoming flow does not force step-by-step completion,
      // so ensure required order timing fields are populated like the
      // previous tabbed workflow.
      const firstSample = SampleTypes.length > 0 ? SampleTypes[0] : null;
      const inferredDate =
        firstSample?.sampleXML?.collectionDate ||
        newOrderFormValues.sampleOrderItems.receivedDateForDisplay ||
        configurationProperties?.currentDateAsText ||
        "";
      const inferredTime =
        firstSample?.sampleXML?.collectionTime ||
        newOrderFormValues.sampleOrderItems.receivedTime ||
        configurationProperties?.currentTimeAsText ||
        "";

      const urlParams = new URLSearchParams(window.location.search);
      const labNumber = urlParams.get("labNumber");
      const externalOrderNumber =
        externalOrderInfo.externalOrderNumber || urlParams.get("ID") || "";
      const externalOrderNumbers =
        externalOrderInfo.externalOrderNumbers || externalOrderNumber;

      newOrderFormValues = {
        ...newOrderFormValues,
        sampleOrderItems: {
          ...newOrderFormValues.sampleOrderItems,
          externalOrderNumber: externalOrderNumber,
          externalOrderNumbers: externalOrderNumbers,
          labNo: labNumber,
          requestDate:
            newOrderFormValues.sampleOrderItems.requestDate || inferredDate,
          receivedDateForDisplay:
            newOrderFormValues.sampleOrderItems.receivedDateForDisplay ||
            inferredDate,
          receivedTime:
            newOrderFormValues.sampleOrderItems.receivedTime || inferredTime,
          nextVisitDate:
            newOrderFormValues.sampleOrderItems.nextVisitDate || inferredDate,
        },
      };
      if (shouldFinalizeState) {
        setOrderFormValues((prev) => ({
          ...prev,
          ...newOrderFormValues,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            ...newOrderFormValues.sampleOrderItems,
          },
          sampleXML: prev.sampleXML,
          referralItems: prev.referralItems,
          useReferral: prev.useReferral,
        }));
        setSamples(SampleTypes);
      }

      //TODO not translated over for 3.0 Unsure if needed
      // parseCrossPanels(
      //   order.crosspanel,
      //   crossSampleTypeMap,
      //   crossSampleTypeOrderMap,
      // );
      // parseCrossTests(
      //   order.crosstest,
      //   crossSampleTypeMap,
      //   crossSampleTypeOrderMap,
      // );
      // populateCrossPanelsAndTests(CrossPanels, CrossTests, '${entryDate}');
      // displaySampleTypes('${entryDate}');

      // if (SampleTypes.length > 0) sampleClicked(1);
    } else {
      alert(message);
    }

    // if (attemptAutoSave) {
    // let validToSave =  patientFormValid() && sampleEntryTopValid();
    // if (validToSave) {
    //   savePage();
    // }
    // }
  };

  const parsePatient = (newOrderFormValues, patient) => {
    newOrderFormValues.patientProperties = {
      ...newOrderFormValues.patientProperties,
      guid: patient.guid,
      healthRegion: patient.healthRegion || "",
      healthDistrict: patient.healthDistrict || "",
      healthRegionName: patient.healthRegion || "",
      healthDistrictName: patient.healthDistrict || "",
      city: patient.city || "",
    };
  };

  const parseRequester = (newOrderFormValues, requester) => {
    const providerId = requester.personId;
    if (providerId) {
      newOrderFormValues.sampleOrderItems = {
        ...newOrderFormValues.sampleOrderItems,
        providerId: providerId,
      };
      getFromOpenElisServer(
        "/rest/practitioner?providerId=" + providerId,
        (data) => {
          if (!data?.person) {
            return;
          }
          setOrderFormValues((prev) => ({
            ...prev,
            sampleOrderItems: {
              ...prev.sampleOrderItems,
              providerId: data.id,
              providerPersonId: data.person.id,
              providerFirstName: data.person.firstName,
              providerLastName: data.person.lastName,
              providerWorkPhone: data.person.workPhone,
              providerEmail: data.person.email,
              providerFax: data.person.fax,
            },
          }));
        },
      );
    } else {
      newOrderFormValues.sampleOrderItems = {
        ...newOrderFormValues.sampleOrderItems,
        providerFirstName: requester.firstName,
        providerLastName: requester.lastName,
        providerWorkPhone: requester.phone,
        providerEmail: requester.email,
        providerFax: requester.fax,
      };
    }
  };

  const parseRequestingOrg = (newOrderFormValues, requestingOrg) => {
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
      referringSiteId: requestingOrg.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + requestingOrg.id,
      () => {
        // Departments loaded - handled elsewhere
      },
    );
  };

  const parseLocation = (newOrderFormValues, location) => {
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
      referringSiteId: location.id,
    };
    getFromOpenElisServer(
      "/rest/departments-for-site?refferingSiteId=" + location.id,
      () => {
        // Departments loaded - handled elsewhere
      },
    );
  };

  // Referring diagnoses are synced from OpenMRS and shown (read-only) in the existing
  // Provisional Clinical Diagnosis field - never sent back on submit. When orders are
  // grouped (multiple IDs), diagnoses from every loaded order are merged in, since each
  // order in the group may correlate to a different OpenMRS visit/diagnosis. The
  // referringDiagnoses array is retained purely as a "synced from OpenMRS" flag so the
  // Provisional Clinical Diagnosis field can render read-only with a source-aware label.
  const parseReferringDiagnoses = (newOrderFormValues, referringDiagnoses) => {
    const diagnosisNodes = !referringDiagnoses.referringDiagnosis
      ? []
      : referringDiagnoses.referringDiagnosis instanceof Array
        ? referringDiagnoses.referringDiagnosis
        : [referringDiagnoses.referringDiagnosis];
    const newTexts = diagnosisNodes
      .map((node) => node.text)
      .filter((text) => text && text.trim().length > 0);
    if (newTexts.length === 0) {
      return;
    }
    const existingTexts =
      newOrderFormValues.sampleOrderItems.referringDiagnoses || [];
    const mergedTexts = Array.from(new Set([...existingTexts, ...newTexts]));
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
      referringDiagnoses: mergedTexts,
      provisionalClinicalDiagnosis: mergedTexts.join("; "),
    };
  };

  // Emergency contact (next of kin) is read-only, synced from OpenMRS - never sent back on
  // submit. Rendered as "Name - phone" in a read-only field. The first loaded order in a
  // grouped set wins (all share the same patient, so the contact is identical).
  const parseEmergencyContact = (newOrderFormValues, emergencyContact) => {
    if (newOrderFormValues.sampleOrderItems.emergencyContact) {
      return;
    }
    const name = (emergencyContact.name || "").trim();
    const phone = (emergencyContact.phone || "").trim();
    const display = phone ? (name ? name + " - " + phone : phone) : name;
    if (display.length === 0) {
      return;
    }
    newOrderFormValues.sampleOrderItems = {
      ...newOrderFormValues.sampleOrderItems,
      emergencyContact: display,
    };
  };

  const parseSampletypes = (
    newOrderFormValues,
    sampletypes,
    SampleTypes,
    serviceRequestIdForThisOrder,
  ) => {
    let index = 0;
    for (let i = 0; i < sampletypes.length; i++) {
      index = parseSampletype(
        index,
        sampletypes[i].sampleType,
        SampleTypes,
        serviceRequestIdForThisOrder,
      );
    }
  };

  const parseSampletype = (
    index,
    sampleType,
    SampleTypes,
    serviceRequestIdForThisOrder,
  ) => {
    let sampleTypeName = sampleType.name;
    let sampleTypeId = sampleType.id;
    let panels = sampleType.panels;
    let tests = sampleType.tests;
    let collection = sampleType.collection;
    let sampleTypeInList = sampleTypeMap[sampleTypeId];
    if (!sampleTypeInList) {
      index++;
      SampleTypes[index - 1] = newSampleType(
        sampleTypeId,
        sampleTypeName,
        index,
      );
      sampleTypeMap[sampleTypeId] = SampleTypes[index - 1];
      SampleTypes[index - 1].rowid = index;
      sampleTypeInList = SampleTypes[index - 1];
    }
    let panelnodes = getNodeNamesByTagName(panels, "panel");
    let testnodes = getNodeNamesByTagName(tests, "test");
    let collectionDate = collection.date;
    let collectionTime = collection.time;

    addPanelsToSampleType(
      sampleTypeInList,
      panelnodes,
      serviceRequestIdForThisOrder,
    );
    addTestsToSampleType(
      sampleTypeInList,
      testnodes,
      serviceRequestIdForThisOrder,
    );
    if (collectionDate) {
      sampleTypeInList.sampleXML.collectionDate = collectionDate;
    } else {
      sampleTypeInList.sampleXML.collectionDate =
        configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
          ? configurationProperties.currentDateAsText
          : "";
    }
    if (collectionTime) {
      sampleTypeInList.sampleXML.collectionTime = collectionTime;
    } else {
      sampleTypeInList.sampleXML.collectionTime =
        configurationProperties?.AUTOFILL_COLLECTION_DATE === "true"
          ? configurationProperties.currentTimeAsText
          : "";
    }
    return index;
  };

  // const parseCrossPanels = (
  //   crosspanels,
  //   crossSampleTypeMap,
  //   crossSampleTypeOrderMap,
  // ) => {
  //   for (let i = 0; i < crosspanels.length; i++) {
  //     var crossPanelName = crosspanels[i].name;
  //     var crossPanelId = crosspanels[i].id;
  //     var crossSampleTypes = crosspanels[i].crosssampletypes;

  //     CrossPanels[i] = newCrossPanel(crossPanelId, crossPanelName);
  //     CrossPanels[i].sampleTypes = getNodeNamesByTagName(
  //       crossSampleTypes,
  //       "crosssampletype",
  //     );
  //     CrossPanels[i].typeMap = [CrossPanels[i].sampleTypes.length];

  //     for (let j = 0; j < CrossPanels[i].sampleTypes.length; j = j + 1) {
  //       CrossPanels[i].typeMap[CrossPanels[i].sampleTypes[j].name] = "t";
  //       var sampleType = crossSampleTypeMap[CrossPanels[i].sampleTypes[j].id];

  //       if (sampleType === undefined) {
  //         crossSampleTypeMap[CrossPanels[i].sampleTypes[j].id] =
  //           CrossPanels[i].sampleTypes[j];
  //         sampleTypeOrder = sampleTypeOrder + 1;
  //         crossSampleTypeOrderMap[sampleTypeOrder] =
  //           CrossPanels[i].sampleTypes[j].id;
  //       }
  //     }
  //   }
  // };

  // const parseCrossTests = (
  //   crosstests,
  //   crossSampleTypeMap,
  //   crossSampleTypeOrderMap,
  // ) => {
  //   for (let x = 0; x < crosstests.length; x = x + 1) {
  //     var crossTestName = crosstests[x].name;
  //     var crossSampleTypes = crosstests[x].crosssampletypes;

  //     CrossTests[x] = newCrossTest(crossTestName);
  //     CrossTests[x].sampleTypes = getNodeNamesByTagName(
  //       crossSampleTypes,
  //       "crosssampletype",
  //     );
  //     CrossTests[x].typeMap = [CrossTests[x].sampleTypes.length];
  //     var sTypes = [];
  //     for (var y = 0; y < CrossTests[x].sampleTypes.length; y++) {
  //       //alert(crossTestName + " " + CrossTests[x].sampleTypes[y].id + " testid=" + CrossTests[x].sampleTypes[y].testId);
  //       sTypes[y] = CrossTests[x].sampleTypes[y];
  //       CrossTests[x].typeMap[CrossTests[x].sampleTypes[y].name] = "t";
  //       var sType = crossSampleTypeMap[CrossTests[x].sampleTypes[y].id];

  //       if (sType === undefined) {
  //         crossSampleTypeMap[CrossTests[x].sampleTypes[y].id] =
  //           CrossTests[x].sampleTypes[y];
  //         sampleTypeOrder++;
  //         crossSampleTypeOrderMap[sampleTypeOrder] =
  //           CrossTests[x].sampleTypes[y].id;
  //       }
  //     }
  //     crossTestSampleTypeTestIdMap[crossTestName] = sTypes;
  //   }
  // };

  function addPanelsToSampleType(
    sampleType,
    panelNodes,
    serviceRequestIdForThisOrder,
  ) {
    for (let i = 0; i < panelNodes.length; i++) {
      const panel = panelNodes[i];
      const panelExists = sampleType.panels.some(
        (existingPanel) => existingPanel.id === panel.id,
      );
      if (panelExists) {
        continue;
      }
      sampleType.panels[sampleType.panels.length] = panel;
      if (serviceRequestIdForThisOrder) {
        panel.referringServiceRequestId = serviceRequestIdForThisOrder;
      }
    }
  }
  function addTestsToSampleType(
    sampleType,
    testNodes,
    serviceRequestIdForThisOrder,
  ) {
    for (let i = 0; i < testNodes.length; i++) {
      const testId = testNodes[i].id;
      const exists = sampleType.tests.some((test) => test.id === testId);
      if (!exists) {
        sampleType.tests[sampleType.tests.length] = newTest(
          testId,
          testNodes[i].name,
          serviceRequestIdForThisOrder,
        );
      }
    }
  }

  function getNodeNamesByTagName(elements, tag) {
    //initialize helper objects
    let allTestsMap = {};
    let panelTestsMap = {};

    if (elements[tag] === undefined) {
      return [];
    }
    let nodes =
      elements[tag] instanceof Array ? elements[tag] : [elements[tag]];
    let objList = [];

    for (let j = 0; j < nodes.length; j++) {
      let name = nodes[j].name;
      let id = nodes[j].id;
      if (tag == "panel") {
        objList[j] = newPanel(id, name);
        let testNodes = nodes[j].panelTests;
        if (!testNodes) {
          continue;
        }
        if (testNodes.length === undefined) {
          testNodes = [testNodes];
        }
        for (let x = 0; x < testNodes.length; x++) {
          let ptNodes = testNodes[x].test;
          if (!ptNodes) {
            continue;
          }
          if (ptNodes.length === undefined) {
            ptNodes = [ptNodes];
          }
          for (let y = 0; y < ptNodes.length; y++) {
            let pName = ptNodes[y].name;
            let pId = ptNodes[y].id;
            if (objList[j].tests.length == 0) {
              objList[j].tests = pName;
              objList[j].testIds = pId;
            } else {
              objList[j].tests = objList[j].tests + "," + pName;
              objList[j].testIds = objList[j].testIds + "," + pId;
            }
          }
        }
      } else if (tag == "test") {
        objList[j] = newTest(id, name);
        allTestsMap[id] = name;
      } else if (tag == "crosssampletype") {
        let testtag = nodes[j].testid;
        if (testtag) {
          objList[j] = newCrossSampleType(id, name, testtag);
        } else objList[j] = newCrossSampleType(id, name);
      }
    }

    return objList;
  }

  const newSampleType = (id, name, index) => {
    return {
      index: index,
      sampleRejected: true,
      rejectionReason: "",
      requestReferralEnabled: false,
      referralItems: [],
      sampleTypeId: "" + id,
      sampleXML: {
        collectionDate: "",
        collector: "",
        quantity: "",
        uom: "",
        rejected: false,
        rejectionReason: "",
        collectionTime: "",
      },
      id: "" + id,
      name: name,
      panels: [],
      tests: [],
      // setCrossPanels: "false",
      // setCrossTests: "false",
      // crossPanels: [],
      // crossTests: [],
    };
  };

  const newPanel = (id, name) => {
    return {
      id: "" + id,
      name: name,
      tests: "",
      testIds: "",
    };
  };
  const newTest = (id, name, referringServiceRequestId) => {
    const t = { id: "" + id, name: name };
    if (referringServiceRequestId) {
      t.referringServiceRequestId = referringServiceRequestId;
    }
    return t;
  };
  const newCrossSampleType = (id, name, testId) => {
    return {
      id: "" + id,
      name: name,
      testId: testId,
    };
  };
  const newCrossPanel = (id, name) => {
    return {
      id: "" + id,
      name: name,
      sampleTypes: [],
      typeMap: [],
    };
  };
  const newCrossTest = (name) => {
    return {
      name: name,
      sampleTypes: [],
      typeMap: [],
    };
  };

  const showAlertMessage = (msg, kind) => {
    setNotificationVisible(true);
    addNotification({
      kind: kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: msg,
    });
  };

  const handlePost = (status) => {
    setIsSubmitting(false);
    if (status === 200) {
      showAlertMessage(
        <FormattedMessage id="save.order.success.msg" />,
        NotificationKinds.success,
      );
      setPage(successMsgPageNumber);
    } else {
      showAlertMessage(
        <FormattedMessage id="server.error.msg" />,
        NotificationKinds.error,
      );
    }
  };
  const elementError = (path) => {
    if (errors?.errors?.length > 0) {
      let error = errors.inner?.find((e) => e.path === path);
      if (error) {
        return error.message;
      } else {
        return null;
      }
    }
  };

  const handleSubmitOrderForm = (e) => {
    e.preventDefault();
    // Prevent multiple submissions.
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    if ("years" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.years;
    }
    if ("months" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.months;
    }
    if ("days" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.days;
    }
    if ("healthRegionName" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.healthRegionName;
    }
    if ("healthDistrictName" in orderFormValues.patientProperties) {
      delete orderFormValues.patientProperties.healthDistrictName;
    }
    if ("questionnaire" in orderFormValues.sampleOrderItems) {
      delete orderFormValues.sampleOrderItems.questionnaire;
    }
    // Referring diagnoses are read-only display data synced from OpenMRS (shown in the
    // Provisional Clinical Diagnosis field) and are not a SampleOrderItem property, so
    // strip them before submit to avoid a deserialization error on the backend bean.
    if ("referringDiagnoses" in orderFormValues.sampleOrderItems) {
      delete orderFormValues.sampleOrderItems.referringDiagnoses;
    }
    // Emergency contact is read-only display data synced from OpenMRS and is not a
    // SampleOrderItem property, so strip it before submit (same reason as referringDiagnoses).
    if ("emergencyContact" in orderFormValues.sampleOrderItems) {
      delete orderFormValues.sampleOrderItems.emergencyContact;
    }
    //remove display Lists rom the form
    orderFormValues.sampleOrderItems.priorityList = [];
    orderFormValues.sampleOrderItems.programList = [];
    orderFormValues.sampleOrderItems.referringSiteList = [];
    orderFormValues.initialSampleConditionList = [];
    orderFormValues.testSectionList = [];
    orderFormValues.sampleOrderItems.providersList = [];
    orderFormValues.sampleOrderItems.paymentOptions = [];
    orderFormValues.sampleOrderItems.testLocationCodeList = [];
    console.log(JSON.stringify(orderFormValues));
    postToOpenElisServer(
      "/rest/SamplePatientEntry",
      JSON.stringify(orderFormValues),
      handlePost,
    );
  };

  useEffect(() => {
    if (page === samplePageNumber + 1) {
      attacheSamplesToFormValues();
    }
  }, [page]);

  useEffect(() => {
    if (isIncomingOrderFlow) {
      attacheSamplesToFormValues();
    }
  }, [isIncomingOrderFlow, samples]);

  useEffect(() => {
    console.log(changed);
    const schema = OrderEntryValidationSchema(
      configurationProperties?.PATIENT_NATIONAL_ID_REQUIRED === "true",
    );
    schema
      .validate(orderFormValues, { abortEarly: false })
      .then((validData) => {
        setErrors([]);
        console.debug("Valid Data:", validData);
      })
      .catch((errors) => {
        setErrors(errors);
        console.error("Validation Errors:", errors.errors);
      });
  }, [
    changed,
    orderFormValues,
    configurationProperties?.PATIENT_NATIONAL_ID_REQUIRED,
  ]);

  useEffect(() => {
    const labNumber = new URLSearchParams(window.location.search).get(
      "labNumber",
    );
    setOrderFormValues((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        labNo: labNumber ? labNumber : "",
      },
    }));
  }, []);

  const buildTestReferringServiceRequestMapAttr = (sampleItem) => {
    const pairs = [];
    const seen = new Set();
    const pushPair = (testId, srId) => {
      if (!testId || !srId) {
        return;
      }
      const tid = String(testId).trim();
      const sid = String(srId).trim();
      if (!tid || !sid || seen.has(tid)) {
        return;
      }
      seen.add(tid);
      pairs.push(`${tid}:${sid}`);
    };
    if (sampleItem.tests && sampleItem.tests.length > 0) {
      Object.keys(sampleItem.tests).forEach((i) => {
        const t = sampleItem.tests[i];
        if (t.referringServiceRequestId) {
          pushPair(t.id, t.referringServiceRequestId);
        }
      });
    }
    if (sampleItem.panels && sampleItem.panels.length > 0) {
      Object.keys(sampleItem.panels).forEach((i) => {
        const p = sampleItem.panels[i];
        if (p.referringServiceRequestId && p.testIds) {
          String(p.testIds)
            .split(",")
            .forEach((tid) => {
              pushPair(tid, p.referringServiceRequestId);
            });
        }
      });
    }
    return pairs.length ? pairs.join(",") : "";
  };

  const attacheSamplesToFormValues = () => {
    let sampleXmlString = "";
    let referralItems = [];
    if (samples.length > 0) {
      sampleXmlString = '<?xml version="1.0" encoding="utf-8"?>';
      sampleXmlString += "<samples>";
      let tests = "";
      let panels = "";
      samples.map((sampleItem) => {
        const testIds = new Set();
        if (sampleItem.tests.length > 0) {
          Object.keys(sampleItem.tests).forEach((i) => {
            const testId = sampleItem.tests[i].id;
            if (testId) {
              testIds.add(String(testId));
            }
          });
        }
        if (sampleItem?.panels.length > 0) {
          Object.keys(sampleItem.panels).forEach((i) => {
            const panelTestIds = sampleItem.panels[i].testIds;
            if (panelTestIds) {
              String(panelTestIds)
                .split(",")
                .map((id) => id.trim())
                .filter((id) => id.length > 0)
                .forEach((id) => testIds.add(id));
            }
          });
        }

        if (testIds.size > 0) {
          tests = Array.from(testIds).join(",");

          if (sampleItem?.panels.length > 0) {
            panels = Object.keys(sampleItem.panels)
              .map(function (i) {
                return sampleItem.panels[i].id;
              })
              .join(",");
          }
          // Extract storage location data if present
          const storageLocation = sampleItem.sampleXML?.storageLocation;
          const storageLocationId = storageLocation?.id || "";
          const storageLocationType = storageLocation?.type || "";
          const storagePositionCoordinate =
            storageLocation?.positionCoordinate || "";

          // Extract GPS coordinates data if present
          const gpsLatitude = sampleItem.sampleXML?.gpsLatitude || "";
          const gpsLongitude = sampleItem.sampleXML?.gpsLongitude || "";
          const gpsAccuracy = sampleItem.sampleXML?.gpsAccuracy || "";
          const gpsCaptureMethod = sampleItem.sampleXML?.gpsCaptureMethod || "";

          const testReferringServiceRequestMap =
            buildTestReferringServiceRequestMapAttr(sampleItem);
          const referringMapAttr =
            testReferringServiceRequestMap.length > 0
              ? ` testReferringServiceRequestMap='${testReferringServiceRequestMap}'`
              : "";

          sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' date='${sampleItem.sampleXML.collectionDate}' time='${sampleItem.sampleXML.collectionTime}' collector='${sampleItem.sampleXML.collector}' quantity='${sampleItem.sampleXML.quantity}' uom='${sampleItem.sampleXML.uom}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='${panels}' rejected='${sampleItem.sampleXML.rejected}' rejectReasonId='${sampleItem.sampleXML.rejectionReason}' initialConditionIds='' storageLocationId='${storageLocationId}' storageLocationType='${storageLocationType}' storagePositionCoordinate='${storagePositionCoordinate}' gpsLatitude='${gpsLatitude}' gpsLongitude='${gpsLongitude}' gpsAccuracy='${gpsAccuracy}' gpsCaptureMethod='${gpsCaptureMethod}'${referringMapAttr}/>`;
        }
        if (sampleItem.referralItems.length > 0) {
          const referredInstitutes = Object.keys(sampleItem.referralItems)
            .map(function (i) {
              return sampleItem.referralItems[i].institute;
            })
            .join(",");

          const sentDates = Object.keys(sampleItem.referralItems)
            .map(function (i) {
              return sampleItem.referralItems[i].sentDate;
            })
            .join(",");

          const referralReasonIds = Object.keys(sampleItem.referralItems)
            .map(function (i) {
              return sampleItem.referralItems[i].reasonForReferral;
            })
            .join(",");

          const referrers = Object.keys(sampleItem.referralItems)
            .map(function (i) {
              return sampleItem.referralItems[i].referrer;
            })
            .join(",");
          referralItems.push({
            referrer: referrers,
            referredInstituteId: referredInstitutes,
            referredTestId: tests,
            referredSendDate: sentDates,
            referralReasonId: referralReasonIds,
          });
        }
      });
      sampleXmlString += "</samples>";
    }
    setOrderFormValues((prev) => ({
      ...prev,
      useReferral: true,
      sampleXML: sampleXmlString,
      referralItems: referralItems,
    }));
  };

  const navigateForward = () => {
    if (page <= lastPageNumber && page >= firstPageNumber) {
      setPage(page + 1);
    }
  };

  const navigateBackWards = () => {
    if (page > firstPageNumber) {
      setPage(page + -1);
    }
  };
  const handleTabClickHandler = (e) => {
    setPage(e);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Stack gap={10}>
        <div className="pageContent">
          {notificationVisible === true ? <AlertDialog /> : ""}
          <div className="orderWorkFlowDiv">
            <h2>
              <FormattedMessage id="order.test.request.heading" />
            </h2>
            {!isIncomingOrderFlow && page <= orderPageNumber && (
              <ProgressIndicator
                currentIndex={page}
                className="ProgressIndicator"
                spaceEqually={true}
                onChange={(e) => handleTabClickHandler(e)}
              >
                <ProgressStep
                  complete
                  label={intl.formatMessage({ id: "order.step.patient.info" })}
                />
                <ProgressStep
                  label={intl.formatMessage({
                    id: "order.step.program.selection",
                  })}
                />
                <ProgressStep
                  label={intl.formatMessage({ id: "sample.add.action" })}
                />
                <ProgressStep
                  label={intl.formatMessage({ id: "order.label.add" })}
                />
              </ProgressIndicator>
            )}

            {isIncomingOrderFlow && page !== successMsgPageNumber && (
              <>
                <PatientInfo
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                  error={elementError}
                  setPhoneValidation={setPhoneValidation}
                  isIncomingOrderFlow={isIncomingOrderFlow}
                />
                <OrderEntryAdditionalQuestions
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                />
                <AddSample
                  error={elementError}
                  setSamples={setSamples}
                  samples={samples}
                  isIncomingOrderFlow={isIncomingOrderFlow}
                />
                <AddOrder
                  orderFormValues={orderFormValues}
                  setOrderFormValues={setOrderFormValues}
                  samples={samples}
                  error={elementError}
                  isModifyOrder={false}
                  changed={changed}
                  setChanged={setChanged}
                  isIncomingOrderFlow={isIncomingOrderFlow}
                />
              </>
            )}

            {!isIncomingOrderFlow && page === patientInfoPageNumber && (
              <PatientInfo
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
                error={elementError}
                setPhoneValidation={setPhoneValidation}
                isIncomingOrderFlow={isIncomingOrderFlow}
              />
            )}
            {page === programPageNumber && (
              <OrderEntryAdditionalQuestions
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
              />
            )}
            {page === samplePageNumber && (
              <AddSample
                error={elementError}
                setSamples={setSamples}
                samples={samples}
                isIncomingOrderFlow={isIncomingOrderFlow}
              />
            )}
            {page === orderPageNumber && (
              <AddOrder
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
                samples={samples}
                error={elementError}
                isModifyOrder={false}
                changed={changed}
                setChanged={setChanged}
                isIncomingOrderFlow={isIncomingOrderFlow}
              />
            )}

            {page === successMsgPageNumber && (
              <OrderSuccessMessage
                orderFormValues={orderFormValues}
                setOrderFormValues={setOrderFormValues}
                setSamples={setSamples}
                setPage={setPage}
              />
            )}
            <div className="navigationButtonsLayout">
              {!isIncomingOrderFlow &&
                page !== firstPageNumber &&
                page <= orderPageNumber && (
                  <Button kind="tertiary" onClick={() => navigateBackWards()}>
                    <FormattedMessage id="back.action.button" />
                  </Button>
                )}

              {!isIncomingOrderFlow && page < orderPageNumber && (
                <Button
                  kind="primary"
                  className="forwardButton"
                  onClick={() => navigateForward()}
                >
                  <FormattedMessage id="next.action.button" />
                </Button>
              )}

              {isIncomingOrderFlow && page !== successMsgPageNumber && (
                <Button
                  kind="primary"
                  className="forwardButton"
                  disabled={
                    isSubmitting ||
                    Object.values(phoneValidation).some(
                      (item) => item.status === false,
                    ) ||
                    errors?.errors?.length > 0
                      ? true
                      : false
                  }
                  onClick={handleSubmitOrderForm}
                >
                  <FormattedMessage id="label.button.submit" />
                </Button>
              )}

              {!isIncomingOrderFlow && page === orderPageNumber && (
                <Button
                  kind="primary"
                  className="forwardButton"
                  disabled={
                    isSubmitting ||
                    Object.values(phoneValidation).some(
                      (item) => item.status === false,
                    ) ||
                    errors?.errors?.length > 0
                      ? true
                      : false
                  }
                  onClick={handleSubmitOrderForm}
                >
                  <FormattedMessage id="label.button.submit" />
                </Button>
              )}
            </div>
          </div>
        </div>
      </Stack>
    </>
  );
};

export default Index;
