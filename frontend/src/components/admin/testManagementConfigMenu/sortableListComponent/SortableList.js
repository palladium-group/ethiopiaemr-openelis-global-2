import React, { useEffect, useState } from "react";
import { Draggable } from "@carbon/icons-react";
import { Button, Checkbox } from "@carbon/react";
import { FormattedMessage } from "react-intl";

export const SortableResultSelectionOptionList = ({
  test,
  onSort,
  onRemove,
}) => {
  const [tests, setTests] = useState(test);

  const handleDragStart = (e, index) => {
    e.dataTransfer.setData("dragIndex", index);
  };

  const handleDragOver = (e) => e.preventDefault();

  const handleDrop = (e, dropIndex) => {
    const dragIndex = e.dataTransfer.getData("dragIndex");
    if (dragIndex === dropIndex) return;

    const newItems = [...tests?.items];
    const [draggedItem] = newItems.splice(dragIndex, 1);
    newItems.splice(dropIndex, 0, draggedItem);
    const updatedItems = newItems.map((item, index) => ({
      ...item,
      order: index,
    }));

    setTests({
      ...tests,
      items: updatedItems,
    });

    // const sortedSendout = updatedItems.map((item, index) => {
    //   return {
    //     id: Number(item.id), //if id exisit the only nee udpate
    //     qualifiable: item.qualifiable ?? false, //if qualifiable presed the only send this as ture
    //     order: index,
    //   };
    // });

    onSort({
      ...tests,
      items: updatedItems,
    });
  };

  const handleQualifiableCheckboxClick = (index) => {
    const updatedItems = [...tests?.items];

    updatedItems[index] = {
      ...updatedItems[index],
      qualifiable: !updatedItems[index].qualifiable,
      order: updatedItems[index].order + 1, // Increment order by 1
    };

    setTests(updatedItems);
  };

  const handleNormalCheckboxClick = (index) => {
    // const updatedItems = [...tests?.items];

    // updatedItems[index] = {
    //   ...updatedItems[index],
    //   qualifiable: !updatedItems[index].qualifiable,
    //   order: updatedItems[index].order - 1, // Decrement order by 1
    // };

    // setTests(updatedItems);

    alert(
      <>
        <FormattedMessage id="configuration.selectList.confirmChange" />
      </>,
    );
  };

  useEffect(() => {
    setTests(test);
    onSort(test);
  }, [test]);

  return (
    <div
      style={{
        width: "300px",
        border: "1px solid #ccc",
        padding: "10px",
        margin: "10px",
      }}
    >
      {tests?.description && (
        <h3
          key={tests?.id}
          style={{
            background: "#cce0d0",
            padding: "5px",
            textAlign: "center",
          }}
        >
          {tests?.description}
        </h3>
      )}
      {tests &&
        tests?.items &&
        tests?.items?.length > 0 &&
        tests?.items?.map((item, index) => (
          <div
            key={item.id}
            draggable
            onDragStart={(e) => handleDragStart(e, index)}
            onDragOver={handleDragOver}
            onDrop={(e) => handleDrop(e, index)}
            style={{
              padding: "10px",
              margin: "5px 0",
              background: item.qualifiable ? "#cfc" : "#eee",
              display: "flex",
              alignItems: "center",
              cursor: "grab",
              border: "1px solid #bbb",
            }}
          >
            <Draggable
              aria-label="result-select-values-list-draggable"
              size={24}
              key={item.id}
            />
            {item.value}
          </div>
        ))}
      <div style={{ margin: "10px", textAlign: "center" }}>
        <Checkbox
          id="qualifiable-checkbox"
          labelText={"Qualifiable"}
          onClick={() => handleQualifiableCheckboxClick(index)}
        />
        <br />
        <Checkbox
          id="normal-checkbox"
          labelText={"Normal"}
          onClick={() => handleNormalCheckboxClick(index)}
        />
        <br />
        <Button
          type="button"
          kind="tertiary"
          onClick={() => {
            onRemove();
          }}
        >
          <FormattedMessage id="label.button.remove" />
        </Button>
      </div>
    </div>
  );
};
