import React, { useState } from "react";

import {
  Button,
  Form,
  FormGroup,
  Label,
  Input,
  FormFeedback,
  Modal,
  ModalHeader,
  ModalBody,
} from "reactstrap";

import { useAppSelector, useAppDispatch } from "../../redux/store";
import { selectMessages } from "../../redux/reducers/locale";
import { selectSignIn, logout } from "../../redux/reducers/signin";

import { getServerUrl } from "../../ServerInformation";
let url = getServerUrl();

interface EditCategoryModalProps {
  show: boolean;
  closeModal: () => void;
  id: number;
  name: string;
  setName: (_: string) => void;
  rank: number;
  friends: any[];
}

function EditCategoryModal({
  show,
  closeModal,
  id,
  name,
  setName,
  rank,
  friends,
}: EditCategoryModalProps) {
  const token = useAppSelector(selectSignIn).token;
  const mywishlist = useAppSelector(selectMessages).mywishlist;

  const appDispatch = useAppDispatch();

  const [errorMessage, setErrorMessage] = useState("");

  let onFormSubmit = (e: any) => {
    e.preventDefault();
    let share: string[] = [];
    for (var i = 0; i < e.target.length; i++) {
      const input = e.target[i];
      if (input.type === "checkbox" && input.checked) {
        share.push(input.name);
      }
    }
    if (name === "") {
      return;
    }
    const request = async () => {
      const response = await fetch(url + "/categories/" + id, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: e.target.name.value,
          rank: rank,
          share: share,
        }),
      });
      if (response.status === 200) {
        setErrorMessage("");
        closeModal();
      } else if (response.status === 401) {
        setErrorMessage("");
        appDispatch(logout());
      } else {
        const json = await response.json();
        setErrorMessage(json.error);
      }
    };
    request();
  };

  const names = friends.map((f: any, i: any) => {
    return (
      <FormGroup check>
        <Label check>
          <Input type="checkbox" id={f.id + "-" + i} name={f.otherUser.name} />
          {f.otherUser.name}
        </Label>
      </FormGroup>
    );
  });

  return (
    <Modal isOpen={show} toggle={closeModal}>
      <ModalHeader toggle={closeModal}>
        {mywishlist.updateCategoryModalTitle}
      </ModalHeader>
      <ModalBody>
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <Form onSubmit={onFormSubmit}>
          <FormGroup>
            <Label>{mywishlist.name}</Label>
            <Input
              invalid={name.length === 0}
              name="name"
              placeholder={mywishlist.name}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
          </FormGroup>
          {names && (
            <FormGroup name="share">
              <Label>{mywishlist.sharedWith}</Label>
              {names}
            </FormGroup>
          )}
          <Button
            disabled={name.length === 0}
            color="primary"
            block
            type="submit"
          >
            {mywishlist.updateModalButton}
          </Button>
        </Form>
      </ModalBody>
    </Modal>
  );
}

export default EditCategoryModal;
