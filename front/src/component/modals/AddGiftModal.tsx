import React, { useState, useRef } from "react";

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
  Spinner,
} from "reactstrap";
import ReactCrop, { Crop, PixelCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";

import { useAppSelector, useAppDispatch } from "../../redux/store";
import { selectMessages } from "../../redux/reducers/locale";
import { selectSignIn, logout } from "../../redux/reducers/signin";

import { TO_RADIANS, SCALE, centerAspectCrop } from "../helpers/image";

import { getServerUrl } from "../../ServerInformation";
let url = getServerUrl();

interface AddGiftModalProps {
  show: boolean;
  closeModal: () => void;
  categories: any[];
  friendName: string | null;
}

function AddGiftModal({
  show,
  closeModal,
  categories,
  friendName,
}: AddGiftModalProps) {
  const token = useAppSelector(selectSignIn).token;
  const mywishlist = useAppSelector(selectMessages).mywishlist;

  const appDispatch = useAppDispatch();

  const [name, setName] = useState("");
  const [shouldCheckName, setShouldCheckName] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const [imgSrc, setImgSrc] = useState("");
  const imgRef = useRef<HTMLImageElement>(null);
  const [rotate, setRotate] = useState(0);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop>();

  const [loadingImage, setLoadingImage] = useState(false);

  const loadImage = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setCrop(undefined);
      const reader = new FileReader();
      reader.addEventListener("load", () =>
        setImgSrc(reader.result?.toString() || ""),
      );
      reader.readAsDataURL(e.target.files[0]);
    }
  };

  const onImageLoad = (e: React.SyntheticEvent<HTMLImageElement>) => {
    const { width, height } = e.currentTarget;
    setCrop(centerAspectCrop(width, height));
  };

  let rotateImage = () => {
    let newRotate = rotate + 90;
    if (newRotate === 360) newRotate = 0;
    setRotate(newRotate);
  };

  let createServerFile = async (formData: FormData) => {
    const image = imgRef.current;
    if (image) {
      if (!completedCrop) {
        throw new Error("Crop canvas does not exist");
      }
      const scaleX = image.naturalWidth / image.width;
      const scaleY = image.naturalHeight / image.height;
      const canvas = document.createElement("canvas");
      canvas.width = Math.floor(completedCrop.width * scaleX);
      canvas.height = Math.floor(completedCrop.height * scaleY);
      const cropX = completedCrop.x * scaleX;
      const cropY = completedCrop.y * scaleY;
      const rotateRads = rotate * TO_RADIANS;
      const centerX = image.naturalWidth / 2;
      const centerY = image.naturalHeight / 2;
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        throw new Error("No 2d context");
      }

      // 5) Move the crop origin to the canvas origin (0,0)
      ctx.translate(-cropX, -cropY);
      // 4) Move the origin to the center of the original position
      ctx.translate(centerX, centerY);
      // 3) Rotate around the origin
      ctx.rotate(rotateRads);
      // 2) Scale the image
      ctx.scale(SCALE, SCALE);
      // 1) Move the center of the image to the origin (0,0)
      ctx.translate(-centerX, -centerY);
      ctx.drawImage(
        image,
        0,
        0,
        image.naturalWidth,
        image.naturalHeight,
        0,
        0,
        image.naturalWidth,
        image.naturalHeight,
      );

      const blob: any = await new Promise((resolve) =>
        canvas.toBlob(resolve, "image/png"),
      );
      formData.append("0", blob, "image.png");
    }
  };

  let storeFileOnServer = async () => {
    const formData = new FormData();
    await createServerFile(formData);
    const response = await fetch(url + "/files", {
      method: "post",
      headers: { Authorization: `Bearer ${token}` },
      body: formData,
    });
    if (response.status === 401) {
      appDispatch(logout());
    } else if (response.status === 202) {
      const json = await response.json();
      return json.name;
    } else {
      const json = await response.json();
      console.error(json);
      setErrorMessage(json.error);
      return null;
    }
  };

  const onFormSubmit = (e: any) => {
    setLoadingImage(true);
    e.preventDefault();
    if (name === "") {
      setShouldCheckName(true);
      return;
    }

    const request = async () => {
      const serverFileName = await storeFileOnServer();

      let append_url = friendName ? "?forUser=" + friendName : "";

      const response = await fetch(url + "/gifts" + append_url, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: name,
          description: e.target.description.value,
          price: e.target.price.value,
          whereToBuy: e.target.whereToBuy.value,
          categoryId: e.target.categoryId.value,
          picture: serverFileName,
        }),
      });
      if (response.status === 200) {
        setErrorMessage("");
        setShouldCheckName(false);
        setLoadingImage(false);
        setName("");
        setImgSrc("");
        setRotate(0);
        closeModal();
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        setErrorMessage(json.error);
        setLoadingImage(false);
      }
    };
    request();
  };

  const options = categories.map((cag: any, index: any) => {
    let value = cag.category;
    return (
      <option key={index} value={value.id}>
        {value.name}
      </option>
    );
  });

  return (
    <Modal isOpen={show} toggle={closeModal}>
      <ModalHeader toggle={closeModal}>
        {mywishlist.addGiftModalTitle}
      </ModalHeader>
      <ModalBody>
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <Form onSubmit={onFormSubmit}>
          <FormGroup>
            <Label>{mywishlist.name}</Label>
            <Input
              invalid={shouldCheckName && name.length === 0}
              name="name"
              placeholder={mywishlist.name}
              value={name}
              onChange={(e) => {
                setShouldCheckName(true);
                setName(e.target.value);
              }}
            />
            <FormFeedback>{mywishlist.nameErrorMessage}</FormFeedback>
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.description}</Label>
            <Input
              type="textarea"
              name="description"
              placeholder={mywishlist.description}
            />
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.price}</Label>
            <Input name="price" placeholder="10" />
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.whereToBuy}</Label>
            <Input
              name="whereToBuy"
              placeholder={mywishlist.whereToBuyPlaceholder}
            />
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.category}</Label>
            <Input type="select" name="categoryId">
              {options}
            </Input>
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.image}</Label>
            <Input type="file" onChange={(e) => loadImage(e)} />
          </FormGroup>
          {!!imgSrc && (
            <ReactCrop
              crop={crop}
              onChange={(_, percentCrop) => setCrop(percentCrop)}
              onComplete={(c) => setCompletedCrop(c)}
              minHeight={100}
              minWidth={100}
            >
              <img
                ref={imgRef}
                alt="Gift"
                src={imgSrc}
                style={{ transform: `scale(${SCALE}) rotate(${rotate}deg)` }}
                onLoad={onImageLoad}
              />
            </ReactCrop>
          )}
          {!!imgSrc && (
            <Button color="primary" onClick={rotateImage}>
              Rotate
            </Button>
          )}
          {!!imgSrc && <br />}
          {!!imgSrc && <br />}
          <Button
            disabled={shouldCheckName && name.length === 0}
            color="primary"
            block
            type="submit"
          >
            <Spinner hidden={!loadingImage} size="sm" />{" "}
            {mywishlist.addModalButton}
          </Button>
        </Form>
      </ModalBody>
    </Modal>
  );
}

export default AddGiftModal;
