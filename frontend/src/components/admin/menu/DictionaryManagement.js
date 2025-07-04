import { ArrowLeft, ArrowRight } from "@carbon/icons-react";
import {
  Button,
  Column,
  DataTable,
  Dropdown,
  Form,
  Grid,
  Heading,
  Modal,
  Pagination,
  Search,
  Section,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectRow,
  TextInput,
} from "@carbon/react";
import React, { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import "../../Style.css";

function DictionaryManagement() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { reloadConfiguration } = useContext(ConfigurationContext);

  const componentMounted = useRef(false);
  const dirtyFieldsRef = useRef(new Set());

  const [dictionaryMenuList, setDictionaryMenuList] = useState([]);
  const [categoryDescription, setCategoryDescription] = useState([]);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [isMobile, setIsMobile] = useState(window.innerWidth < 530);
  const [modalOpen, setModalOpen] = useState(false);
  const [editMode, setEditMode] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const initialFormState = {
    dictionaryNumber: "",
    category: null,
    dictionaryEntry: "",
    localAbbreviation: "",
    isActive: null,
    loincCode: "",
  };
  const [formState, setFormState] = useState(initialFormState);
  const [originalValues, setOriginalValues] = useState(initialFormState);

  const yesOrNo = [
    { id: "Y", value: "Y" },
    { id: "N", value: "N" },
  ];

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 530);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    loadDictionaryMenu();
    loadCategories();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadDictionaryMenu = () => {
    getFromOpenElisServer("/rest/DictionaryMenu", (res) => {
      if (componentMounted.current && res?.menuList) {
        const menuList = res.menuList.map((item) => ({
          id: item.id,
          dictEntry: item.dictEntry,
          localAbbreviation: item.localAbbreviation,
          isActive: item.isActive,
          loincCode: item.loincCode || "-",
          categoryName:
            item.dictionaryCategory?.categoryName || "not available",
        }));
        setDictionaryMenuList(menuList);
      }
    });
  };

  const loadCategories = () => {
    getFromOpenElisServer("/rest/dictionary-categories", (res) => {
      if (componentMounted.current) {
        const categories = Array.isArray(res)
          ? res.map((cat) => ({
              id: cat.id,
              description: cat.description || cat.categoryName || "Unnamed",
            }))
          : [];
        setCategoryDescription(categories);
      }
    });
  };

  const handleInputChange = (field, value) => {
    setFormState((prev) => ({ ...prev, [field]: value }));
    if (originalValues[field] !== value) dirtyFieldsRef.current.add(field);
  };

  const openAddModal = () => {
    setFormState(initialFormState);
    setEditMode(true);
    setModalOpen(true);
  };

  const openEditModal = () => {
    if (selectedRowIds.length !== 1) return;
    const selectedItem = dictionaryMenuList.find(
      (item) => item.id === selectedRowIds[0],
    );
    if (selectedItem) {
      setFormState({
        dictionaryNumber: selectedItem.id,
        category:
          categoryDescription.find(
            (cat) => cat.id === selectedItem.category?.id,
          ) || null,
        dictionaryEntry: selectedItem.dictEntry,
        localAbbreviation: selectedItem.localAbbreviation,
        isActive: yesOrNo.find((y) => y.id === selectedItem.isActive) || null,
        loincCode: selectedItem.loincCode === "-" ? "" : selectedItem.loincCode,
      });
      setOriginalValues({
        dictionaryNumber: selectedItem.id,
        category: selectedItem.category,
        dictionaryEntry: selectedItem.dictEntry,
        localAbbreviation: selectedItem.localAbbreviation,
        isActive: selectedItem.isActive,
        loincCode: selectedItem.loincCode === "-" ? "" : selectedItem.loincCode,
      });
      setEditMode(false);
      setModalOpen(true);
    }
  };

  const handleSubmit = () => {
    const payload = {
      id: formState.dictionaryNumber,
      selectedDictionaryCategoryId: formState.category?.id,
      dictEntry: formState.dictionaryEntry,
      localAbbreviation: formState.localAbbreviation,
      isActive: formState.isActive?.id,
      loincCode: formState.loincCode.trim() || null,
      dirtyFormFields:
        dirtyFieldsRef.current.size > 0
          ? `;${[...dirtyFieldsRef.current].join(";")}`
          : "",
    };
    postToOpenElisServerFullResponse(
      "/rest/Dictionary",
      JSON.stringify(payload),
      handleResponse,
    );
  };

  const handleUpdate = () => {
    handleSubmit();
  };

  const handleResponse = (res) => {
    setNotificationVisible(true);
    if (res.status === "200" || res.status === "201") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.add.edited.msg" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.add.edited.msg" }),
      });
    }
    window.location.reload();
  };

  const renderCell = (cell, row) => {
    if (cell.info.header === "select") {
      return (
        <TableSelectRow
          key={cell.id}
          id={cell.id}
          checked={selectedRowIds.includes(row.id)}
          name="selectRowRadio"
          ariaLabel="selectRow"
          onSelect={(e) => {
            e.stopPropagation();
            setSelectedRowIds(
              selectedRowIds.includes(row.id)
                ? selectedRowIds.filter((id) => id !== row.id)
                : [...selectedRowIds, row.id],
            );
          }}
        />
      );
    }
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  return (
    <div className="adminPageContent">
      {notificationVisible && <AlertDialog />}
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
          {
            label: "dictionary.label.modify",
            link: "/MasterListsPage#DictionaryManagement",
          },
        ]}
      />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="dictionary.label.modify" />
            </Heading>
          </Section>

          <Form>
            <Button onClick={openAddModal} disabled={!editMode}>
              <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.add" />
            </Button>
            <Button
              onClick={openEditModal}
              disabled={selectedRowIds.length !== 1}
            >
              <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
            </Button>

            <Modal
              open={modalOpen}
              size="sm"
              onRequestClose={() => setModalOpen(false)}
              modalHeading={editMode ? "Add Dictionary" : "Edit Dictionary"}
              primaryButtonText={editMode ? "Add" : "Update"}
              secondaryButtonText="Cancel"
              onRequestSubmit={editMode ? handleSubmit : handleUpdate}
            >
              <TextInput
                id="dictNumber"
                labelText="Dictionary Number"
                disabled
                value={formState.dictionaryNumber}
              />
              <Dropdown
                id="description"
                items={categoryDescription}
                titleText="Dictionary Category"
                itemToString={(item) => item?.description || ""}
                onChange={({ selectedItem }) =>
                  handleInputChange("category", selectedItem)
                }
                selectedItem={formState.category}
              />
              <TextInput
                id="dictEntry"
                labelText="Dictionary Entry"
                value={formState.dictionaryEntry}
                onChange={(e) =>
                  handleInputChange("dictionaryEntry", e.target.value)
                }
              />
              <Dropdown
                id="isActive"
                items={yesOrNo}
                titleText="Is Active"
                itemToString={(item) => item?.id || ""}
                onChange={({ selectedItem }) =>
                  handleInputChange("isActive", selectedItem)
                }
                selectedItem={formState.isActive}
              />
              <TextInput
                id="localAbbrev"
                labelText="Local Abbreviation"
                value={formState.localAbbreviation}
                onChange={(e) =>
                  handleInputChange("localAbbreviation", e.target.value)
                }
              />
              <TextInput
                id="loincCode"
                labelText="LOINC Code"
                value={formState.loincCode}
                onChange={(e) => handleInputChange("loincCode", e.target.value)}
              />
            </Modal>
          </Form>
        </Column>
      </Grid>

      <DataTable
        rows={dictionaryMenuList.slice((page - 1) * pageSize, page * pageSize)}
        headers={[
          { key: "select", header: "Select" },
          { key: "categoryName", header: "Category" },
          { key: "dictEntry", header: "Dictionary Entry" },
          { key: "localAbbreviation", header: "Abbreviation" },
          { key: "isActive", header: "Active" },
          { key: "loincCode", header: "LOINC" },
        ]}
      >
        {({ rows, headers, getHeaderProps, getTableProps }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id}>
                    {row.cells.map((cell) => renderCell(cell, row))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
      <Pagination
        onChange={({ page, pageSize }) => {
          setPage(page);
          setPageSize(pageSize);
        }}
        page={page}
        pageSize={pageSize}
        pageSizes={[10, 20]}
        totalItems={dictionaryMenuList.length}
      />
    </div>
  );
}

export default DictionaryManagement;
