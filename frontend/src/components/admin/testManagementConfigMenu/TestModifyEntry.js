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

  const componentMounted = useRef(false);

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
                        console.log(test.id);
                      }}
                    >
                      {test.value}
                    </ClickableTile>
                  </Column>
                ))}
              </>
            ) : (
              <></>
            )}
          </Grid>
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
