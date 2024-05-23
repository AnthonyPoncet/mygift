import React, { useState, useRef, useEffect } from "react";
import { Button, Input, Label, Form, FormGroup, Spinner } from "reactstrap";

import { useNavigate } from "react-router-dom";

import { useAppSelector, useAppDispatch } from "../redux/store";
import {
  addMessage,
  selectErrorMessage,
  clearMessage,
} from "../redux/reducers/error";
import { selectSignIn, logout, accountUpdated } from "../redux/reducers/signin";
import { selectMessages } from "../redux/reducers/locale";

import { isMobile } from "react-device-detect";

import ReactCrop, {
  centerCrop,
  makeAspectCrop,
  Crop,
  PixelCrop,
} from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

const TO_RADIANS = Math.PI / 180;

function ManageAccount() {
  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const picture = useAppSelector(selectSignIn).picture;

  const manageAccount = useAppSelector(selectMessages).manageAccount;

  const errorMessage = useAppSelector(selectErrorMessage);

  const appDispatch = useAppDispatch();
  let navigate = useNavigate();

  const [imgSrc, setImgSrc] = useState("");
  const imgRef = useRef<HTMLImageElement>(null);
  const [rotate, setRotate] = useState(0);
  const [crop, setCrop] = useState<Crop>();
  const [completedCrop, setCompletedCrop] = useState<PixelCrop>();

  const [loadingImage, setLoadingImage] = useState(false);

  const scale = 1;
  const aspect = 1;

  useEffect(() => {
    if (token && picture) {
      const request = async () => {
        const response = await fetch(url + "/files/" + picture, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 404) {
          console.error("file '" + picture + "' could not be found on server");
          return;
        }
        if (response.status === 401) {
          console.error("Unauthorized. Disconnect and redirect to connect");
          appDispatch(logout());
          return;
        }
        if (response.status === 500) {
          console.error("Internal server error: " + response);
          return;
        }

        response.blob().then((blob) => {
          let url = window.URL.createObjectURL(blob);
          setImgSrc(url);
        });
      };
      request();
    }
  }, [token, picture, appDispatch]);

  if (token && username) {
    let centerAspectCrop = (
      mediaWidth: number,
      mediaHeight: number,
      aspect: number,
    ) => {
      return centerCrop(
        makeAspectCrop(
          {
            unit: "%",
            width: 100,
          },
          aspect,
          mediaWidth,
          mediaHeight,
        ),
        mediaWidth,
        mediaHeight,
      );
    };

    let rotateImage = () => {
      let newRotate = rotate + 90;
      if (newRotate === 360) newRotate = 0;
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
        ctx.scale(scale, scale);
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
        appDispatch(addMessage(json.error));
        return null;
      }
    };

    let onImageLoad = (e: React.SyntheticEvent<HTMLImageElement>) => {
      const { width, height } = e.currentTarget;
      setCrop(centerAspectCrop(width, height, 1));
    };

    let onFormSubmit = (e: any) => {
      setLoadingImage(true);
      e.preventDefault();
      const request = async () => {
        const serverFileName = await storeFileOnServer();
        const response = await fetch(url + "/users", {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            name: e.target.name.value,
            picture: serverFileName,
          }),
        });
        if (response.status === 202) {
          appDispatch(clearMessage());
          appDispatch(
            accountUpdated({
              username: e.target.name.value,
              picture: serverFileName,
            }),
          ).then(() => {
            navigate("../");
          });
        } else if (response.status === 401) {
          appDispatch(logout());
        } else {
          const json = await response.json();
          setLoadingImage(false);
          console.error(json);
          appDispatch(addMessage(json.error));
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
            <Input name="name" defaultValue={username} />
          </FormGroup>
          <FormGroup>
            <Label>{manageAccount.profile_picture}</Label>
            <Input type="file" onChange={(e) => loadImage(e)} />
          </FormGroup>
          {!!imgSrc && (
            <ReactCrop
              crop={crop}
              onChange={(_, percentCrop) => setCrop(percentCrop)}
              onComplete={(c) => setCompletedCrop(c)}
              aspect={aspect}
              minHeight={100}
            >
              <img
                ref={imgRef}
                alt="Profile"
                src={imgSrc}
                style={{ transform: `scale(${scale}) rotate(${rotate}deg)` }}
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
          <Button color="primary" type="submit" disabled={loadingImage}>
            <Spinner hidden={!loadingImage} size="sm" /> {manageAccount.save}
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
