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
import "./PathologyDashboard.css";
import PageBreadCrumb from "../common/PageBreadCrumb";
import PathologyCaseWorkflowRail from "./PathologyCaseWorkflowRail";

/**
 * Pathology case page: patient/order context + workflow rail only.
 * Legacy form panels (blocks/slides CRUD, reports, techniques, IHC referral, classic Save)
 * were removed; step APIs on the rail own the workflow.
 */
function PathologyCaseView() {
  const componentMounted = useRef(false);
  const { pathologySampleId } = useParams();
  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [pathologySampleInfo, setPathologySampleInfo] = useState({});
  const [loading, setLoading] = useState(true);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
  ];

  const setInitialPathologySampleInfo = (sample) => {
    const next = { ...sample };
    if (
      hasRole(userSessionDetails, "Pathologist") &&
      !next.assignedPathologistId &&
      next.status === "READY_PATHOLOGIST"
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
      "/rest/pathology/caseView/" + pathologySampleId,
      setInitialPathologySampleInfo,
    );
    return () => {
      componentMounted.current = false;
    };
  }, [pathologySampleId]);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="pathology.label.title" />
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
            <PathologyCaseWorkflowRail
              pathologySampleId={pathologySampleId}
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

export default PathologyCaseView;
