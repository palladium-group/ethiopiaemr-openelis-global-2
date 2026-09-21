import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import { Heading, Grid, Column, Section, Loading } from "@carbon/react";
import { getFromOpenElisServer, hasRole } from "../utils/Utils";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { FormattedMessage } from "react-intl";
import PatientHeader from "../common/PatientHeader";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import "../pathology/PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CytologyCaseWorkflowRail from "./CytologyCaseWorkflowRail";

/**
 * Cytology case page: patient/order context + workflow rail only.
 * Legacy form panels (slides CRUD, reports, adequacy, Bethesda diagnosis, classic Save)
 * were removed; step APIs on the rail own the workflow.
 */
function CytologyCaseView() {
  const componentMounted = useRef(false);
  const { cytologySampleId } = useParams();
  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [pathologySampleInfo, setPathologySampleInfo] = useState({});
  const [loading, setLoading] = useState(true);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "cytology.label.dashboard", link: "/CytologyDashboard" },
  ];

  const setInitialPathologySampleInfo = (sample) => {
    const next = { ...sample };
    if (
      hasRole(userSessionDetails, "CytoPathologist") &&
      !next.assignedPathologistId &&
      next.status === "READY_FOR_CYTOPATHOLOGIST"
    ) {
      next.assignedPathologistId = userSessionDetails.userId;
      next.assignedPathologist =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    if (!next.assignedTechnicianId) {
      next.assignedTechnicianId = userSessionDetails.userId;
      next.assignedTechnician =
        userSessionDetails.lastName + " " + userSessionDetails.firstName;
    }
    setPathologySampleInfo(next);
    setLoading(false);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/cytology/caseView/" + cytologySampleId,
      setInitialPathologySampleInfo,
    );
    return () => {
      componentMounted.current = false;
    };
  }, [cytologySampleId]);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="cytology.label.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <PatientHeader
                id={pathologySampleInfo.patientPK}
                lastName={pathologySampleInfo.lastName}
                firstName={pathologySampleInfo.firstName}
                gender={pathologySampleInfo.sex}
                age={pathologySampleInfo.age}
                orderDate={pathologySampleInfo.requestDate}
                referringFacility={pathologySampleInfo.referringFacility}
                department={pathologySampleInfo.department}
                requester={pathologySampleInfo.requester}
                accesionNumber={pathologySampleInfo.labNumber}
                className="patient-header2"
                isOrderPage={true}
              />
            </Section>
          </Section>
          <Section>
            <Section>
              <div className="patient-header2">
                <QuestionnaireResponse
                  questionnaireResponse={
                    pathologySampleInfo.programQuestionnaireResponse
                  }
                />
              </div>
            </Section>
          </Section>
        </Column>
      </Grid>

      <Grid fullWidth={true} className="orderLegendBody">
        {notificationVisible === true ? <AlertDialog /> : null}
        {loading && <Loading description="Loading case..." />}
        <Column lg={16} md={8} sm={4}>
          {!loading ? (
            <CytologyCaseWorkflowRail
              cytologySampleId={cytologySampleId}
              pathologySampleInfo={pathologySampleInfo}
              onCaseUpdated={(updated) => {
                setPathologySampleInfo((prev) => ({
                  ...prev,
                  ...updated,
                }));
              }}
            />
          ) : null}
        </Column>
      </Grid>
    </>
  );
}

export default CytologyCaseView;
