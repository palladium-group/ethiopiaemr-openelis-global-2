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
  TextInput,
  Checkbox,
  Modal,
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

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "label.resultSelectList",
    link: "/MasterListsPage#ResultSelectListAdd",
  },
];

function ResultSelectListAdd() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [englishLangPost, setEnglishLangPost] = useState("");
  const [frenchLangPost, setFrenchLangPost] = useState("");
  const [inputError, setInputError] = useState(false);
  const [ResultSelectListRes, setResultSelectListRes] = useState({});
  const [resultTestsList, setResultTestsList] = useState([]);
  const [resultTestsDirectory, setResultTestsDirectory] = useState([]);

  const componentMounted = useRef(false);

  const handleResultSelectTestListCall = () => {
    if (!englishLangPost || !frenchLangPost) {
      setInputError(true);
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/ResultSelectListAdd",
      JSON.stringify({
        nameEnglish: englishLangPost,
        nameFrench: frenchLangPost,
      }),
      (res) => {
        handlePostResultSelectListCallBack(res);
      },
    );
  };

  const handlePostResultSelectListCallBack = (res) => {
    if (res) {
      setResultSelectListRes(res);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handlePostSaveResultSelectListCall = () => {
    if (!englishLangPost || !frenchLangPost) {
      setInputError(true);
      return;
    }
    postToOpenElisServerJsonResponse(
      "/rest/SaveResultSelectList",
      JSON.stringify({
        nameEnglish: englishLangPost,
        nameFrench: frenchLangPost,
      }),
      (res) => {
        handlePostSaveResultSelectListCallBack(res);
      },
    );
  };

  const handlePostSaveResultSelectListCallBack = (res) => {
    if (res) {
      setResultSelectListRes(res);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleResultSelectTestList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setResultSelectListRes(res);
      if (res?.tests && res?.testDictionary) {
        setResultTestsList(res?.tests);
        setResultTestsDirectory(res?.testDictionary);
      }
    }
  };

  const handleTestNameResultValueSetToSpecificTest = (testId, checked) => {};

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/ResultSelectListAdd`,
      handleResultSelectTestList,
    );
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
                  <FormattedMessage id="configuration.selectList.header" />
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
                    <Section>
                      <Heading>
                        <FormattedMessage id="configuration.selectList.description" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={4} md={4} sm={4}>
              <>
                <FormattedMessage id="english.label" />
                <span className="requiredlabel">*</span> :
              </>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`eng`}
                labelText=""
                hideLabel
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
            <Column lg={4} md={4} sm={4}>
              <>
                <FormattedMessage id="french.label" />
                <span className="requiredlabel">*</span> :
              </>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id={`fr`}
                labelText=""
                hideLabel
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
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="configuration.selectList.assign.header" />
                  </Heading>
                </Section>
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
                    <Section>
                      <Heading>
                        <FormattedMessage id="configuration.selectList.assign.description" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
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
                    <Section>
                      <Heading>
                        <FormattedMessage id="availabletests.title" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          {resultTestsList?.length > 0 && (
            <Grid fullWidth={true}>
              {resultTestsList?.map((test) => (
                <Column lg={4} md={4} sm={4}>
                  <Checkbox
                    id={test.id}
                    labelText={test.description}
                    checked={false}
                    onChange={(_, { checked }) => {
                      handleTestNameResultValueSetToSpecificTest(
                        test.id,
                        checked,
                      );
                    }}
                  />
                </Column>
              ))}
            </Grid>
          )}
          <br />
          <Grid fullWidth={true}>
            <Column lg={4} md={4} sm={4}>
              <Button
                onClick={() => {
                  if (englishLangPost && frenchLangPost !== "") {
                    setIsConfirmModalOpen(true);
                  } else {
                    setInputError(true);
                  }
                }}
                type="button"
                kind="primary"
              >
                <FormattedMessage id="next.action.button" />
              </Button>{" "}
              <Button type="button" kind="tertiary">
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
        </div>
        <button
          onClick={() => {
            handleResultSelectTestListCall();
          }}
        >
          ResultSelectListCall
        </button>
        <button
          onClick={() => {
            console.log(ResultSelectListRes);
          }}
        >
          ResultSelectListResLog
        </button>
        <button
          onClick={() => {
            console.log(resultTestsList);
          }}
        >
          ResultTestsListLog
        </button>
        <button
          onClick={() => {
            console.log(resultTestsDirectory);
          }}
        >
          ResultTestsDirectoryLog
        </button>
      </div>

      <Modal
        open={isConfirmModalOpen}
        size="md"
        modalHeading={
          <FormattedMessage id="configuration.selectList.assign.new" />
        }
        primaryButtonText={<FormattedMessage id="label.button.save" />}
        secondaryButtonText={<FormattedMessage id="label.button.cancel" />}
        onRequestSubmit={() => {
          setIsConfirmModalOpen(false);
          handlePostSaveResultSelectListCall();
        }}
        onRequestClose={() => {
          setIsConfirmModalOpen(false);
          window.location.reload();
        }}
        preventCloseOnClickOutside={true}
        shouldSubmitOnEnter={true}
      >
        <Grid fullWidth={true}>
          <Column lg={4} md={4} sm={4}>
            {/* renderSortablelistType3 */}
          </Column>
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(ResultSelectListAdd);
