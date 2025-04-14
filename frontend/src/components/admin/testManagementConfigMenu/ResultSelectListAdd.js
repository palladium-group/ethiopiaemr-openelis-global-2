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
import { SortableResultSelectionOptionList } from "./sortableListComponent/SortableList.js";

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
  const [bothFilled, setBothFilled] = useState(false);
  const [englishLangPost, setEnglishLangPost] = useState("");
  const [frenchLangPost, setFrenchLangPost] = useState("");
  const [inputError, setInputError] = useState(false);
  const [ResultSelectListRes, setResultSelectListRes] = useState({});
  const [resultTestsList, setResultTestsList] = useState([]);
  const [resultTestsDirectory, setResultTestsDirectory] = useState([]);
  const [testSelectListJson, setTestSelectListJson] = useState([]);
  const [testSelectListJsonPost, setTestSelectListJsonPost] = useState([]);

  // [{id: testId, items : [{id: itemId, order: sortingOrder++ },{ qualifiable: boolean, sortingOrder++ }]}]
  // normal checkbox === hard alert
  // qualifiable {if checked then in arrary qualifiable : true else false}

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
      if (res?.tests && res?.testDictionary) {
        setResultTestsList(res?.tests);
        setResultTestsDirectory(res?.testDictionary);
      }
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
        testSelectListJson: JSON.stringify({
          items: JSON.stringify(testSelectListJsonPost),
        }),
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
    }
  };

  const handleTestNameResultValueSetToSpecificTest = (
    testId,
    testDescription,
    checked,
  ) => {
    if (checked) {
      const relatedItems = resultTestsDirectory[testId];
      if (Array.isArray(relatedItems)) {
        const itemsWithOrder = relatedItems.map((item, index) => ({
          id: item.id,
          value: item.value,
          order: index,
        }));

        const newTestObj = {
          id: testId,
          description: testDescription,
          items: itemsWithOrder,
        };

        setTestSelectListJson((prev) => [...prev, newTestObj]);
      }
    } else {
      setTestSelectListJson((prev) =>
        prev.filter((t) => String(t.id) !== String(id)),
      );
      setTestSelectListJsonPost((prev) =>
        prev.filter((t) => String(t.id) !== String(id)),
      );
    }
  };

  // const getTestValueNamesWithTestId = (testId) => {
  //   const testObj = resultTestsList.find((test) => test.id === testId);
  //   let testValueNames = [];
  //   if (testObj) {
  //     testValueNames.push({
  //       id: testObj.id,
  //       description: testObj.description,
  //     });
  //     return testValueNames;
  //   }
  //   return [];
  // };

  const handleRemove = (id) => {
    setTestSelectListJson((prev) =>
      prev.filter((test) => String(test.id) !== String(id)),
    );
    setTestSelectListJsonPost((prev) =>
      prev.filter((test) => String(test.id) !== String(id)),
    );
  };

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
            <Column lg={4} md={4} sm={4}>
              <Button
                onClick={() => {
                  handleResultSelectTestListCall();
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
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
          <br />
          <hr />
          {resultTestsList &&
            resultTestsList?.length > 0 &&
            resultTestsDirectory && (
              <>
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
                      <Column lg={4} md={4} sm={4} key={test.id}>
                        <Checkbox
                          id={test.id}
                          labelText={test.description}
                          checked={
                            !!testSelectListJson.find(
                              (obj) => String(obj.id) === String(test.id),
                            )
                          }
                          onChange={(_, { checked }) => {
                            handleTestNameResultValueSetToSpecificTest(
                              test.id,
                              test.description,
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
                    <Button
                      type="button"
                      kind="tertiary"
                      onClick={() => {
                        window.location.reload();
                      }}
                    >
                      <FormattedMessage id="label.button.cancel" />
                    </Button>
                  </Column>
                </Grid>
              </>
            )}
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
        <button
          onClick={() => {
            console.log(testSelectListJson);
            console.log(testSelectListJsonPost);
          }}
        >
          testSelectListJson
        </button>
      </div>

      <Modal
        open={isConfirmModalOpen}
        size="lg"
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
        <button
          onClick={() => {
            console.log(testSelectListJson);
            console.log(testSelectListJsonPost);
          }}
        >
          testSelectListJson
        </button>
        <Grid fullWidth={true}>
          {testSelectListJson.map((test, index) => (
            <Column lg={4} md={4} sm={4} key={test.id}>
              <SortableResultSelectionOptionList
                test={test}
                onSort={(updatedTestSelectListJson) => {
                  setTestSelectListJson((prev) => {
                    const newArrangement = [...prev];
                    newArrangement[index] = updatedTestSelectListJson;
                    return newArrangement;
                  });
                  setTestSelectListJsonPost((prev) => {
                    const newArrangement = [...prev];
                    newArrangement[index] = {
                      ...updatedTestSelectListJson,
                      items: updatedTestSelectListJson.items.map(
                        ({ value, ...rest }) => rest,
                      ),
                    };
                    delete newArrangement[index].description;
                    return newArrangement;
                  });
                }}
                onRemove={() => {
                  handleRemove(test.id);
                }}
              />
            </Column>
          ))}
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(ResultSelectListAdd);

// remove description and value from the testSelectListJson list
// check box fix on sortableList component
// check the testSelectListJsonPost list
