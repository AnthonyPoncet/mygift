import React, { useState } from "react";

import { Button, Form, Modal, ModalBody, ModalHeader } from "reactstrap";

import { useAppSelector, useAppDispatch } from "../../redux/store";
import { selectMessages } from "../../redux/reducers/locale";
import { selectSignIn, logout } from "../../redux/reducers/signin";

import { getServerUrl } from "../../ServerInformation";
let url = getServerUrl();

interface DeleteGiftModalProps {
  show: boolean;
  closeModal: () => void;
  giftId: number;
  received: string;
  not_wanted: string;
}

function DeleteGiftModal({
  show,
  closeModal,
  giftId,
  received,
  not_wanted,
}: DeleteGiftModalProps) {
  const token = useAppSelector(selectSignIn).token;
  const mywishlist = useAppSelector(selectMessages).mywishlist;

  const appDispatch = useAppDispatch();

  const [errorMessage, setErrorMessage] = useState("");

  const onFormSubmit = (e: any) => {
    e.preventDefault();

    const request = async () => {
      const response = await fetch(
        url + "/gifts/" + giftId + "?status=" + e.nativeEvent.submitter.value,
        { method: "delete", headers: { Authorization: `Bearer ${token}` } },
      );
      if (response.status === 202) {
        setErrorMessage("");
        closeModal();
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        console.error(json);
        setErrorMessage(json.error);
      }
    };
    request();
  };

  return (
    <Modal isOpen={show} toggle={closeModal}>
      <ModalHeader toggle={closeModal}>
        {mywishlist.deleteGiftModalTitle}
      </ModalHeader>
      <ModalBody>
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <Form onSubmit={onFormSubmit}>
          <Button color="primary" type="submit" value="RECEIVED">
            {received}
          </Button>{" "}
          <Button color="primary" type="submit" value="NOT_WANTED">
            {not_wanted}
          </Button>
        </Form>
      </ModalBody>
    </Modal>
  );
}

export default DeleteGiftModal;
