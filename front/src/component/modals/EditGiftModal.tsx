import React, { useState, useEffect, useRef } from "react";

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

interface EditGiftModalProps {
  show: boolean;
  closeModal: () => void;
  categories: any[];
  name: string;
  setName: (_: string) => void;
  description: string;
  setDescription: (_: string) => void;
  price: string;
  setPrice: (_: string) => void;
  whereToBuy: string;
  setWhereToBuy: (_: string) => void;
  categoryId: number;
  picture: string;
  giftId: number;
  rank: number;
}

function EditGiftModal({
  show,
  closeModal,
  categories,
  name,
  setName,
  description,
  setDescription,
  price,
  setPrice,
  whereToBuy,
  setWhereToBuy,
  categoryId,
  picture,
  giftId,
  rank,
}: EditGiftModalProps) {
  const token = useAppSelector(selectSignIn).token;

  const mywishlist = useAppSelector(selectMessages).mywishlist;
  const imageEdition = useAppSelector(selectMessages).imageEdition;

  const appDispatch = useAppDispatch();

  const [errorMessage, setErrorMessage] = useState("");

  const [imgSrc, setImgSrc] = useState("");
  const imgRef = useRef<HTMLImageElement>(null);
  const [rotate, setRotate] = useState(0);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop>();

  const [loadingImage, setLoadingImage] = useState(false);
  const [sendingImage, setSendingImage] = useState(false);

  useEffect(() => {
    setImgSrc("");
    setRotate(0);
    setCrop(undefined);
    setErrorMessage("");

    if (token && picture) {
      setLoadingImage(true);
      const request = async () => {
        const response = await fetch(
          url + "/files/" + picture + "/not_compressed",
          {
            headers: { Authorization: `Bearer ${token}` },
          },
        );
        if (response.status === 404) {
          console.error("file '" + picture + "' could not be found on server");
          setLoadingImage(false);
          return;
        }
        if (response.status === 401) {
          console.error("Unauthorized. Disconnect and redirect to connect");
          appDispatch(logout());
          return;
        }
        if (response.status === 500) {
          console.error("Internal server error: " + response);
          setLoadingImage(false);
          return;
        }

        response.blob().then((blob) => {
          let url = window.URL.createObjectURL(blob);
          setImgSrc(url);
          setLoadingImage(false);
        });
      };
      request();
    }
  }, [token, picture, appDispatch]);

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

  let rotateImage = (amount: number) => {
    let newRotate = rotate + amount;
    if (newRotate >= 360) newRotate -= 360;
    if (newRotate <= -360) newRotate += 360;
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
    setSendingImage(true);
    e.preventDefault();
    if (name === "") {
      return;
    }

    const request = async () => {
      const serverFileName = await storeFileOnServer();
      const response = await fetch(url + "/gifts/" + giftId, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          name: name,
          description: description,
          price: price,
          whereToBuy: whereToBuy,
          categoryId: e.target.categoryId.value,
          picture: serverFileName,
          rank: rank,
        }),
      });
      if (response.status === 200) {
        setSendingImage(false);
        closeModal();
      } else if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        setErrorMessage(json.error);
        setSendingImage(false);
      }
    };
    request();
  };

  const options = categories.map((cag: any, index: any) => {
    let value = cag.category;
    if (categoryId === value.id) {
      return (
        <option key={index} value={value.id} selected>
          {value.name}
        </option>
      );
    } else {
      return (
        <option key={index} value={value.id}>
          {value.name}
        </option>
      );
    }
  });

  return (
    <Modal isOpen={show} toggle={closeModal}>
      <ModalHeader toggle={closeModal}>
        {mywishlist.updateGiftModalTitle}
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
          <FormGroup>
            <Label>{mywishlist.description}</Label>
            <Input
              type="textarea"
              name="description"
              placeholder={mywishlist.description}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.price}</Label>
            <Input
              name="price"
              placeholder="10"
              value={price}
              onChange={(e) => setPrice(e.target.value)}
            />
          </FormGroup>
          <FormGroup>
            <Label>{mywishlist.whereToBuy}</Label>
            <Input
              name="whereToBuy"
              placeholder={mywishlist.whereToBuyPlaceholder}
              value={whereToBuy}
              onChange={(e) => setWhereToBuy(e.target.value)}
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
          {loadingImage && (
            <p>
              <Spinner hidden={!loadingImage} size="sm" /> Loading previous
              image...
            </p>
          )}
          {!!imgSrc && (
            <FormGroup style={{ display: "flex", flexDirection: "column" }}>
              <ReactCrop
                style={{ marginBottom: "5px" }}
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
              <div style={{ display: "flex" }}>
                <Button
                  style={{ width: "45%", marginRight: "10%" }}
                  color="dark"
                  onClick={() => rotateImage(-90)}
                >
                  {imageEdition.rotateLeft}
                </Button>
                <Button
                  style={{ width: "45%" }}
                  color="dark"
                  onClick={() => rotateImage(90)}
                >
                  {imageEdition.rotateRight}
                </Button>
              </div>
            </FormGroup>
          )}
          <Button
            disabled={name.length === 0 || loadingImage || sendingImage}
            color="primary"
            block
            type="submit"
          >
            <Spinner hidden={!sendingImage} size="sm" />{" "}
            {mywishlist.updateModalButton}
          </Button>
        </Form>
      </ModalBody>
    </Modal>
  );
}

export default EditGiftModal;
