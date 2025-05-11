import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
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
  UnorderedList,
  ListItem,
  TextInput,
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
import CustomCheckBox from "../../common/CustomCheckBox.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.panel.manage",
    link: "/MasterListsPage#SampleTypeManagement",
  },
  {
    label: "configuration.panel.create",
    link: "/MasterListsPage#PanelCreate",
  },
];

function PanelCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [bothFilled, setBothFilled] = useState(false);
  const [englishLangPost, setEnglishLangPost] = useState("");
  const [frenchLangPost, setFrenchLangPost] = useState("");
  const [inputError, setInputError] = useState(false);
  const [panelCreateList, setPanelCreateList] = useState({});

  const componentMounted = useRef(false);

  const handlePanelCreateList = (response) => {
    if (!response) {
      setIsLoading(true);
    } else {
      setPanelCreateList(response);
    }
  };

  const handlePanelCreateListCall = () => {};

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/PanelCreate`, handlePanelCreateList);
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

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
                  <FormattedMessage id="banner.menu.patientEdit" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.panel.create" />
                </Heading>
              </Section>
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
                      <FormattedMessage id="panel.new" />
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
            <Column lg={8} md={4} sm={4}>
              <>
                <FormattedMessage id="english.label" />
                <span className="requiredlabel">*</span> :
              </>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id={`eng`}
                labelText=""
                hideLabel
                disabled={bothFilled}
                value={`${englishLangPost}` || ""}
                onChange={(e) => {
                  setEnglishLangPost(e.target.value);
                  setInputError(false);
                }}
                required
                invalid={inputError}
                invalidText={<FormattedMessage id="required.invalidtext" />}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <>
                <FormattedMessage id="french.label" />
                <span className="requiredlabel">*</span> :
              </>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id={`fr`}
                labelText=""
                hideLabel
                disabled={bothFilled}
                value={`${frenchLangPost}` || ""}
                onChange={(e) => {
                  setFrenchLangPost(e.target.value);
                  setInputError(false);
                }}
                required
                invalid={inputError}
                invalidText={<FormattedMessage id="required.invalidtext" />}
              />
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={8} md={8} sm={4}>
              <Button
                onClick={() => {
                  handlePanelCreateListCall();
                  setBothFilled(true);
                  setInputError(false);
                }}
                type="button"
                kind="primary"
              >
                <FormattedMessage id="next.action.button" />
              </Button>{" "}
              <Button
                type="button"
                kind="tertiary"
                onClick={() => {
                  window.location.reload();
                }}
              >
                <FormattedMessage id="label.button.previous" />
              </Button>
            </Column>
          </Grid>
          <br />
          <hr />
        </div>
        <button
          onClick={() => {
            console.log(panelCreateList);
          }}
        >
          panelCreateList
        </button>
      </div>
    </>
  );
}

export default injectIntl(PanelCreate);
