import React, { useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import {
  Button,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  MultiSelect,
  FileUploader,
  Grid,
  Column,
  InlineLoading,
  Section,
  Heading,
  Tile,
  Modal,
  InlineNotification,
  FileUploaderDropContainer,
  FileUploaderItem,
  Loading,
} from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import {
  NoteBookFormValues,
  NoteBookInitialData,
} from "../formModel/innitialValues/NoteBookFormValues";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerForPDF,
  postToOpenElisServer,
  hasRole,
  toBase64,
} from "../utils/Utils";
import SearchPatientForm from "../patient/SearchPatientForm";

const NoteBookEntryForm = () => {
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "notebook.label.dashboard", link: "/NoteBookDashboard" },
  ];
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { notebookid } = useParams();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [statuses, setStatuses] = useState([]);
  const [types, setTypes] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [noteBookData, setNoteBookData] = useState(NoteBookInitialData);
  const [noteBookForm, setNoteBookForm] = useState(NoteBookFormValues);
  const [sampleList, setSampleList] = useState([]);
  const [uploadedFiles, setUploadedFiles] = useState([]);

  const [addedSampleIds, setAddedSampleIds] = useState([]);

  const handleSubmit = () => {
    if (isSubmitting) {
      return;
    }
    setIsSubmitting(true);
    noteBookForm.title = noteBookData.title;
    noteBookForm.type = noteBookData.type;
    noteBookForm.project = noteBookData.project;
    noteBookForm.objective = noteBookData.objective;
    noteBookForm.protocol = noteBookData.protocol;
    noteBookForm.content = noteBookData.content;
    noteBookForm.patientId = noteBookData.patientId;
    noteBookForm.technicianId = noteBookData.technicianId;
    noteBookForm.sampleIds = noteBookData.samples.map((entry) =>
      Number(entry.id),
    );
    noteBookForm.pages = noteBookData.pages;
    noteBookForm.files = noteBookData.files;
    console.log(JSON.stringify(noteBookForm));
    var url =
      notebookid != 0
        ? "/rest/notebook/update/" + notebookid
        : "/rest/notebook/create";
    postToOpenElisServer(url, JSON.stringify(noteBookForm), handleSubmited);
  };

  const handleSubmited = (status) => {
    setIsSubmitting(false);
    setNotificationVisible(true);
    if (status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Succesfuly saved",
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Duplicate Calculation Name or Error while saving",
      });
    }
    window.location.reload();
  };

  // Add sample to noteBookData.samples
  const handleAddSample = (sample) => {
    if (addedSampleIds.includes(sample.id)) return; // prevent duplicates

    setNoteBookData((prev) => ({
      ...prev,
      samples: [...prev.samples, sample],
    }));
    setAddedSampleIds((prev) => [...prev, sample.id]);
  };

  // Remove sample from selected samples
  const handleRemoveSample = (sampleId) => {
    setNoteBookData((prev) => ({
      ...prev,
      samples: prev.samples.filter((s) => s.id !== sampleId),
    }));
    setAddedSampleIds((prev) => prev.filter((id) => id !== sampleId));
  };

  const [showPageModal, setShowPageModal] = useState(false);
  const [newPage, setNewPage] = useState({
    title: "",
    content: "",
    instructions: "",
  });
  const [pageError, setPageError] = useState("");

  // Open modal
  const openPageModal = () => {
    setNewPage({ title: "", content: "", instructions: "" });
    setPageError("");
    setShowPageModal(true);
  };

  // Close modal
  const closePageModal = () => setShowPageModal(false);

  // Handle modal input changes
  const handlePageChange = (e) => {
    const { name, value } = e.target;
    setNewPage((prev) => ({ ...prev, [name]: value }));
  };

  // Add new page to noteBookData.pages
  const handleAddPage = () => {
    if (!newPage.title.trim() || !newPage.content.trim()) {
      setPageError("Title and Content are required.");
      return;
    }
    setNoteBookData((prev) => ({
      ...prev,
      pages: [...prev.pages, newPage],
    }));
    setShowPageModal(false);
  };

  // Remove page by index
  const handleRemovePage = (index) => {
    setNoteBookData((prev) => ({
      ...prev,
      pages: prev.pages.filter((_, i) => i !== index),
    }));
  };

  const handleAddFiles = async (event) => {
    const newFiles = Array.from(event.target.files);

    // convert files to base64
    const fileForms = await Promise.all(
      newFiles.map(async (file) => {
        const base64 = await toBase64(file);
        return {
          base64File: base64,
          fileType: file.type,
        };
      }),
    );

    setNoteBookData((prev) => ({
      ...prev,
      files: [...prev.files, ...fileForms],
    }));

    // update UI list (and mark them as complete)
    setUploadedFiles((prev) => [
      ...prev,
      ...newFiles.map((f) => ({ file: f, status: "complete" })),
    ]);
  };

  const handleRemoveFile = (index) => {
    setNoteBookData((prev) => ({
      ...prev,
      files: prev.files.filter((_, i) => i !== index),
    }));
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const getSelectedPatient = (patient) => {
    setNoteBookData({
      ...noteBookData,
      patientId: patient.patientPK,
    });

    getFromOpenElisServer(
      "/rest/notebook/samples?patientId=" + patient.patientPK,
      setSampleList,
    );
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_STATUS", setStatuses);
    getFromOpenElisServer("/rest/displayList/NOTEBOOK_EXPT_TYPE", setTypes);

    if (notebookid != 0) {
      setLoading(true);
      getFromOpenElisServer(
        "/rest/notebook/view//" + notebookid,
        loadInitialData,
      );
    }
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadInitialData = (data) => {
    if (componentMounted.current) {
      setNoteBookData(data);
      setLoading(false);
    }
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="notebook.label.formEntry" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      <Grid fullWidth={true} className="orderLegendBody">
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                {" "}
                <FormattedMessage id="notebook.label.basicinfo" />
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="entryTitle"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.title",
                    })}
                    <span className="requiredlabel">*</span>
                  </>
                }
                placeholder={intl.formatMessage({
                  id: "notebook.label.title",
                })}
                value={noteBookData.title}
                type="text"
                onChange={(e) => {
                  setNoteBookData({ ...noteBookData, title: e.target.value });
                }}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>

            <Column lg={8} md={2} sm={2}>
              <Select
                id="experimenttype"
                name="experimenttype"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.experimentType",
                    })}
                    <span className="requiredlabel">*</span>
                  </>
                }
                value={noteBookData.type || ""}
                onChange={(event) => {
                  setNoteBookData({
                    ...noteBookData,
                    type: event.target.value,
                  });
                }}
              >
                <SelectItem />
                {types.map((type, index) => {
                  return (
                    <SelectItem key={index} text={type.value} value={type.id} />
                  );
                })}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="entryProject"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.project",
                    })}
                    <span className="requiredlabel">*</span>
                  </>
                }
                placeholder={intl.formatMessage({
                  id: "notebook.label.project",
                })}
                value={noteBookData.project}
                type="text"
                onChange={(e) => {
                  setNoteBookData({ ...noteBookData, project: e.target.value });
                }}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="objective"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.objective",
                    })}
                    <span className="requiredlabel">*</span>
                  </>
                }
                placeholder={intl.formatMessage({
                  id: "notebook.label.objective",
                })}
                value={noteBookData.objective}
                type="text"
                onChange={(e) => {
                  setNoteBookData({
                    ...noteBookData,
                    objective: e.target.value,
                  });
                }}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="protocol"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.protocol",
                    })}
                  </>
                }
                placeholder={intl.formatMessage({
                  id: "notebook.label.protocol",
                })}
                value={noteBookData.protocol}
                type="text"
                onChange={(e) => {
                  setNoteBookData({
                    ...noteBookData,
                    protocol: e.target.value,
                  });
                }}
              />
            </Column>
          </Grid>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <h5>
                {" "}
                <FormattedMessage id="notebook.label.experimentDoc" />
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="content"
                labelText={
                  <>
                    {intl.formatMessage({
                      id: "notebook.label.content",
                    })}
                  </>
                }
                placeholder={intl.formatMessage({
                  id: "notebook.label.content",
                })}
                value={noteBookData.content}
                type="text"
                onChange={(e) => {
                  setNoteBookData({ ...noteBookData, content: e.target.value });
                }}
              />
            </Column>
          </Grid>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br></br>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <hr></hr>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br></br>
        </Column>
        <Column lg={8} md={8} sm={4}>
          <h5>
            {" "}
            <FormattedMessage id="notebook.label.pages" />
          </h5>
        </Column>
        <Column lg={8} md={8} sm={4}>
          <Button onClick={openPageModal} kind="primary">
            <FormattedMessage id="notebook.label.addpage" />
          </Button>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <br></br>
        </Column>
        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            {noteBookData.pages.map((page, index) => (
              <Column key={index} lg={16} md={8} sm={4}>
                <Grid fullWidth={true} className="gridBoundary">
                  <Column lg={16} md={8} sm={4}>
                    <h4>{page.title}</h4>
                    <p>
                      <strong>Instructions:</strong> {page.instructions}
                    </p>
                    <p>
                      <strong>Content:</strong> {page.content}
                    </p>
                    <Button
                      kind="danger--tertiary"
                      size="sm"
                      onClick={() => handleRemovePage(index)}
                    >
                      Remove
                    </Button>
                  </Column>
                </Grid>
              </Column>
            ))}
          </Grid>
        </Column>
        <Modal
          open={showPageModal}
          modalHeading="Add New Page"
          primaryButtonText="Add Page"
          secondaryButtonText="Cancel"
          onRequestClose={closePageModal}
          onRequestSubmit={handleAddPage}
        >
          {pageError && (
            <InlineNotification
              kind="error"
              title="Error"
              subtitle={pageError}
            />
          )}
          <TextInput
            id="title"
            name="title"
            labelText="Title"
            value={newPage.title}
            onChange={handlePageChange}
            required
          />
          <TextArea
            id="instructions"
            name="instructions"
            labelText="Instructions"
            value={newPage.instructions}
            onChange={handlePageChange}
          />
          <TextArea
            id="content"
            name="content"
            labelText="Content"
            value={newPage.content}
            onChange={handlePageChange}
            required
          />
        </Modal>

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <h5>
                {" "}
                <FormattedMessage id="notebook.label.sampleManagement" />
              </h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <SearchPatientForm
                getSelectedPatient={getSelectedPatient}
              ></SearchPatientForm>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <h5>Available Samples</h5>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <Grid className="gridBoundary">
                {sampleList.length === 0 && (
                  <Column lg={16} md={8} sm={4}>
                    <InlineNotification
                      kind="info"
                      title="No samples found"
                      subtitle="No samples were returned from the backend"
                    />
                  </Column>
                )}
                {sampleList.map((sample) => (
                  <Column key={sample.id} lg={16} md={8} sm={12}>
                    <Tile style={{ marginBottom: "1rem" }}>
                      <h4
                        style={{
                          fontWeight: "bold",
                          textTransform: "uppercase",
                        }}
                      >
                        {sample.sampleType}
                      </h4>
                      <p>
                        <strong>Date Collected:</strong>{" "}
                        {sample.collectionDate || "N/A"}
                      </p>
                      <p>
                        <strong>Results Recorded:</strong>{" "}
                        {sample.results.length}
                      </p>
                      <Button
                        kind="secondary"
                        disabled={addedSampleIds.includes(sample.id)}
                        onClick={() => handleAddSample(sample)}
                      >
                        {addedSampleIds.includes(sample.id) ? "Added" : "Add"}
                      </Button>
                    </Tile>
                  </Column>
                ))}
              </Grid>
            </Column>

            <Column lg={16} md={8} sm={4}>
              {noteBookData.samples.length > 0 && (
                <>
                  <h5>Selected Samples</h5>
                  <Grid className="gridBoundary">
                    {noteBookData.samples.map((sample) => (
                      <Column
                        key={sample.id || Math.random()}
                        lg={16}
                        md={8}
                        sm={4}
                      >
                        <Tile style={{ marginBottom: "1rem" }}>
                          <h4
                            style={{
                              fontWeight: "bold",
                              textTransform: "uppercase",
                            }}
                          >
                            {sample.sampleType}
                          </h4>
                          <p>
                            <strong>Date Collected:</strong>{" "}
                            {sample.collectionDate || "N/A"}
                          </p>
                          <p>
                            <strong>Results Recorded:</strong>{" "}
                            {sample.results.length}
                          </p>
                          <Button
                            kind="danger--tertiary"
                            size="sm"
                            onClick={() => handleRemoveSample(sample.id)}
                          >
                            Remove
                          </Button>
                        </Tile>
                      </Column>
                    ))}
                  </Grid>
                </>
              )}
            </Column>
          </Grid>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={8} sm={4}>
              <h5> Attachments</h5>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br></br>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <FileUploaderDropContainer
                labelText="Drag and drop files here or click to upload"
                multiple
                onAddFiles={handleAddFiles}
                accept={[".pdf", ".png", ".jpg", ".txt"]}
              />

              {uploadedFiles.map((fileObj, index) => (
                <FileUploaderItem
                  key={index}
                  name={fileObj.file.name}
                  status={fileObj.status} // "complete" ensures no infinite loader
                  onDelete={() => handleRemoveFile(index)}
                />
              ))}

              {noteBookData.files.length > 0 && (
                <Grid style={{ marginTop: "1rem" }}>
                  {noteBookData.files.map((file, index) => (
                    <Column key={index} lg={8} md={8} sm={12}>
                      <Tile style={{ marginBottom: "1rem" }}>
                        <p>
                          <strong>File Type:</strong> {file.fileType}
                        </p>
                        <Button
                          kind="danger--tertiary"
                          size="sm"
                          onClick={() => handleRemoveFile(index)}
                        >
                          Remove
                        </Button>
                      </Tile>
                    </Column>
                  ))}
                </Grid>
              )}
            </Column>
          </Grid>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <Grid fullWidth={true} className="gridBoundary">
            <Button
              kind="danger--tertiary"
              disabled={isSubmitting}
              onClick={() => handleSubmit()}
            >
              Submit
            </Button>
          </Grid>
        </Column>
      </Grid>
      {/* {JSON.stringify(noteBookData)} */}
    </>
  );
};

export default NoteBookEntryForm;
