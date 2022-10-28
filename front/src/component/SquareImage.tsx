import React, { useEffect, useState } from "react";

import "./style/style.css";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectSignIn, logout } from "../redux/reducers/signin";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

interface Props {
  token: string;
  className: string;
  imageName: string;
  size: number;
  alt: string;
  alternateImage: any;
}

function loadImage(
  imageName: string,
  token: string,
  setImage: any,
  appDispatch: any
) {
  if (imageName === undefined || imageName === "") {
    return;
  }

  const request = async () => {
    const response = await fetch(url + "/files/" + imageName, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (response.status === 404) {
      console.error("file '" + imageName + "' could not be found on server");
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
      setImage(url);
    });
  };
  request();
}

function SquareImage(props: Props) {
  const token = useAppSelector(selectSignIn).token;
  const appDispatch = useAppDispatch();

  const [image, setImage] = useState(null);

  useEffect(() => {
    if (token) {
      loadImage(props.imageName, token, setImage, appDispatch);
    }
  }, [props.imageName, token, setImage, appDispatch]);

  const { className, size, alt, alternateImage } = props;
  if (image !== null) {
    return (
      <img
        className={className}
        height={size}
        width={size}
        src={image}
        alt={alt}
      />
    );
  } else {
    return (
      <img
        className={className}
        height={size}
        width={size}
        src={alternateImage}
        alt="Nothing"
      />
    );
  }
}

export default SquareImage;
