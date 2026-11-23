import React, { useContext, useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { Column, Grid, Heading, Loading, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import PatientHeader from "../common/PatientHeader";
import QuestionnaireResponse from "../common/QuestionnaireResponse";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CaseSupplyOrders from "../pathology/CaseSupplyOrders";
import HematopathologyCaseWorkflowRail from "./HematopathologyCaseWorkflowRail";
import "../pathology/PathologyDashboard.css";

/** Hematopathology case page: patient/order context, supplies and the step rail. */
function HematopathologyCaseView() {
  const mounted = useRef(false);
  const { hematopathologySampleId } = useParams();
  const { notificationVisible } = useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [caseInfo, setCaseInfo] = useState({});
  const [loading, setLoading] = useState(true);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "pathology.label.dashboard", link: "/PathologyDashboard" },
  ];

  useEffect(() => {
    if (!hematopathologySampleId || !userSessionDetails?.userId) {
      return;
    }
    mounted.current = true;
    setLoading(true);
    getFromOpenElisServer(
      "/rest/hematopathology/caseView/" + hematopathologySampleId,
      (data) => {
        if (mounted.current && data) {
          setCaseInfo(data);
          setLoading(false);
        }
      },
    );
    return () => {
      mounted.current = false;
    };
  }, [hematopathologySampleId, userSessionDetails?.userId]);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="hematopathology.label.title" />
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
                id={caseInfo.patientPK}
                lastName={caseInfo.lastName}
                firstName={caseInfo.firstName}
                gender={caseInfo.sex}
                age={caseInfo.age}
                orderDate={caseInfo.requestDate}
                referringFacility={caseInfo.referringFacility}
                department={caseInfo.department}
                requester={caseInfo.requester}
                accesionNumber={caseInfo.labNumber}
                className="patient-header2"
                isOrderPage={true}
              />
            </Section>
          </Section>
          <Section>
            <Section>
              <div className="patient-header2">
                <QuestionnaireResponse
                  questionnaireResponse={caseInfo.programQuestionnaireResponse}
                />
              </div>
            </Section>
          </Section>
        </Column>
      </Grid>

      {!loading ? (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <CaseSupplyOrders
              caseType="hematopathology"
              caseId={hematopathologySampleId}
              caseClosed={
                caseInfo.status === "COMPLETED" ||
                caseInfo.status === "REJECTED"
              }
            />
          </Column>
        </Grid>
      ) : null}

      <Grid fullWidth={true} className="orderLegendBody">
        {notificationVisible === true ? <AlertDialog /> : null}
        {loading && <Loading description="Loading case..." />}
        <Column lg={16} md={8} sm={4}>
          {!loading ? (
            <HematopathologyCaseWorkflowRail
              caseId={hematopathologySampleId}
              caseInfo={caseInfo}
              onCaseUpdated={(updated) =>
                setCaseInfo((prev) => ({ ...prev, ...updated }))
              }
            />
          ) : null}
        </Column>
      </Grid>
    </>
  );
}

export default HematopathologyCaseView;
