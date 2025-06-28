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
  ClickableTile,
  Toggle,
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
import { CustomShowGuide } from "./customComponents/CustomShowGuide.js";
import { CustomTestDataDisplay } from "./customComponents/CustomTestDataDisplay.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.test.modify",
    link: "/MasterListsPage#TestModifyEntry",
  },
];

function TestModifyEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);
  const [testMonifyList, setTestModifyList] = useState({});
  const [showGuide, setShowGuide] = useState(false);
  const [selectedTestIdToEdit, setSelectedTestIdToEdit] = useState(null);

  const componentMounted = useRef(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const handleTestModifyEntryList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestModifyList(res);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestModifyEntry`, handleTestModifyEntryList);
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
                  <FormattedMessage id="configuration.test.modify" />
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
                      <FormattedMessage id="label.viewtestCatalog" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
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
          {selectedTestIdToEdit ? (
            <CustomTestDataDisplay
              testToDisplay={testMonifyList?.testCatBeanList?.find(
                (test) => test.id === selectedTestIdToEdit,
              )}
            />
          ) : (
            <>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="Please Select Test to edit!" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Column>
              </Grid>
            </>
          )}
          <br />
          <hr />
          <Grid fullWidth={true}>
            {testMonifyList && testMonifyList?.testList?.length > 0 ? (
              <>
                {testMonifyList?.testList?.map((test) => (
                  <Column
                    style={{ margin: "2px" }}
                    lg={4}
                    md={4}
                    sm={2}
                    key={test.id}
                  >
                    <ClickableTile
                      id={test.id}
                      onClick={() => {
                        setSelectedTestIdToEdit(test.id);
                      }}
                    >
                      {test.value}
                    </ClickableTile>
                  </Column>
                ))}
              </>
            ) : (
              <>
                <Loading
                  description="loading"
                  small={true}
                  withOverlay={true}
                />
              </>
            )}
          </Grid>
          <hr />
          <br />
        </div>
        <button
          onClick={() => {
            console.log(testMonifyList);
          }}
        >
          testMonifyList
        </button>
      </div>
    </>
  );
}

export default injectIntl(TestModifyEntry);
