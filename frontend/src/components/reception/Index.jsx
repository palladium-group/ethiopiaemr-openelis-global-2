import React from "react";
import { Column, Grid, Heading, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ReceptionQueue from "./ReceptionQueue";

const breadcrumbs = [{ label: "home.label", link: "/" }];

const ReceptionPage = () => {
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="reception.header" />
            </Heading>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <ReceptionQueue />
        </Column>
      </Grid>
    </>
  );
};

export default ReceptionPage;
