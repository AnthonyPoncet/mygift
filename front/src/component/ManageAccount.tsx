import React, { useState, useRef, useEffect } from "react";
import {
  Button,
  Input,
  Label,
  Form,
  FormGroup,
  Spinner,
  FormFeedback,
} from "reactstrap";

import { useNavigate } from "react-router-dom";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectSignIn, logout, accountUpdated } from "../redux/reducers/signin";
import { selectMessages } from "../redux/reducers/locale";

import { isMobile } from "react-device-detect";

import ReactCrop, { Crop, PixelCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";

import { TO_RADIANS, SCALE, centerAspectCrop } from "./helpers/image";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function ManageAccount() {
  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const picture = useAppSelector(selectSignIn).picture;
  const stateDateOfBirth = useAppSelector(selectSignIn).dateOfBirth;

  const dateOfBirth =
    stateDateOfBirth === null
      ? ""
      : new Date(
          stateDateOfBirth * 1000 + new Date().getTimezoneOffset() * 60000,
        ).toLocaleDateString();

  const manageAccount = useAppSelector(selectMessages).manageAccount;
  const imageEdition = useAppSelector(selectMessages).imageEdition;

  const [errorMessage, setErrorMessage] = useState("");
  const [name, setName] = useState(username === null ? "" : username);
  const [nameErrorMessage, setNameErrorMessage] = useState(
    manageAccount.usernameEmptyErrorMessage,
  );
  const [nameValid, setNameValid] = useState(true);
  const [dateValid, setDateValid] = useState(true);

  const appDispatch = useAppDispatch();
  let navigate = useNavigate();

  const [imgSrc, setImgSrc] = useState("");
  const imgRef = useRef<HTMLImageElement>(null);
  const [rotate, setRotate] = useState(0);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop>();

  const [loadingImage, setLoadingImage] = useState(false);
  const [sendingImage, setSendingImage] = useState(false);

  useEffect(() => {
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

  if (token && username) {
    let rotateImage = (amount: number) => {
      let newRotate = rotate + amount;
      if (newRotate >= 360) newRotate -= 360;
      if (newRotate <= -360) newRotate += 360;
      setRotate(newRotate);
    };

    let loadImage = (e: React.ChangeEvent<HTMLInputElement>) => {
      if (e.target.files && e.target.files.length > 0) {
        setCrop(undefined);
        const reader = new FileReader();
        reader.addEventListener("load", () =>
          setImgSrc(reader.result?.toString() || ""),
        );
        reader.readAsDataURL(e.target.files[0]);
      }
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

    let onImageLoad = (e: React.SyntheticEvent<HTMLImageElement>) => {
      const { width, height } = e.currentTarget;
      setCrop(centerAspectCrop(width, height));
    };

    let onFormSubmit = (e: any) => {
      setSendingImage(true);
      e.preventDefault();

      if (name.length === 0) {
        setSendingImage(false);
        return;
      }

      const dateOfBirth = e.target.dateOfBirth.value.split("/");
      if (dateOfBirth.length !== 3) {
        setDateValid(false);
        setSendingImage(false);
        return;
      }

      const asDate = new Date(
        dateOfBirth[2],
        dateOfBirth[1] - 1,
        dateOfBirth[0],
      );

      if (
        new Date(asDate).toLocaleDateString() !== e.target.dateOfBirth.value
      ) {
        setDateValid(false);
        setSendingImage(false);
        return;
      }

      setDateValid(true);

      const request = async () => {
        const serverFileName = await storeFileOnServer();

        const response = await fetch(url + "/users", {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name: name,
            picture: serverFileName,
            dateOfBirth:
              (asDate.getTime() - asDate.getTimezoneOffset() * 60000) / 1000,
          }),
        });
        if (response.status === 202) {
          const json = await response.json();
          appDispatch(
            accountUpdated({
              username: json.name,
              picture: json.picture,
              dateOfBirth: json.dateOfBirth,
            }),
          ).then(() => {
            navigate("../");
          });
        } else if (response.status === 401) {
          appDispatch(logout());
        } else if (response.status === 409) {
          setNameValid(false);
          setNameErrorMessage(manageAccount.usernameTakenErrorMessage);
          setSendingImage(false);
        } else {
          const json = await response.json();
          setSendingImage(false);
          setErrorMessage(json.error);
        }
      };
      request();
    };

    return (
      <div style={{ margin: "10px", width: isMobile ? "100%" : "25%" }}>
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <Form onSubmit={onFormSubmit}>
          <FormGroup>
            <Label>{manageAccount.username}</Label>
            <Input
              name="name"
              invalid={!nameValid || name.length === 0}
              value={name}
              onChange={(e) => {
                setNameValid(true);
                setNameErrorMessage(manageAccount.usernameEmptyErrorMessage);
                setName(e.target.value);
              }}
            />
            <FormFeedback>{nameErrorMessage}</FormFeedback>
          </FormGroup>
          <FormGroup>
            <Label>{manageAccount.dateOfBirth}</Label>
            <Input
              name="dateOfBirth"
              defaultValue={dateOfBirth}
              invalid={!dateValid}
              placeholder={manageAccount.dateOfBirthDefault}
            />
            <FormFeedback>{manageAccount.dateOfBirthErrorMessage}</FormFeedback>
          </FormGroup>
          <FormGroup>
            <Label>{manageAccount.profilePicture}</Label>
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
            color="primary"
            type="submit"
            disabled={loadingImage || sendingImage}
          >
            <Spinner hidden={!sendingImage} size="sm" /> {manageAccount.save}
          </Button>
        </Form>
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default ManageAccount;
