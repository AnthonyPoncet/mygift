import React, { useState, useEffect } from "react";

import { useNavigate } from "react-router-dom";

import { useAppSelector, useAppDispatch } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import { selectSignIn, logout } from "../redux/reducers/signin";

import "./style/card-gift.css";
import "./style/home.css";
import SquareImage from "./SquareImage";
import blank_profile_picture from "./image/blank_profile_picture.png";

import { getServerUrl } from "../ServerInformation";
let url = getServerUrl();

function HomePage() {
  const username = useAppSelector(selectSignIn).username;
  const token = useAppSelector(selectSignIn).token;
  const home = useAppSelector(selectMessages).home;

  const appDispatch = useAppDispatch();
  let navigate = useNavigate();

  const [errorMessage, setErrorMessage] = useState("");

  const [events, setEvents] = useState([]);

  //Get events
  useEffect(() => {
    const request = async () => {
      const response = await fetch(url + "/events", {
        method: "GET",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.status === 401) {
        appDispatch(logout());
      } else {
        const json = await response.json();
        if (response.status === 200) {
          setEvents(json);
        } else {
          console.error(json.error);
          setErrorMessage(json.error);
        }
      }
    };
    request();
  }, [token, appDispatch]);

  if (token && username) {
    return (
      <div className="main-home">
        {errorMessage && <p className="auth-error">{errorMessage}</p>}
        <div className="mycard-row">
          {events.map((event: any, index: number) => {
            return (
              <div
                className="mycard"
                style={{
                  cursor: event.kind === "BIRTHDAY" ? "pointer" : "inherit",
                }}
                onClick={() => {
                  if (event.kind === "BIRTHDAY")
                    navigate("/friend/" + event.name);
                }}
                key={index}
              >
                {event.kind === "CHRISTMAS" && (
                  <>
                    <SquareImage
                      token={token}
                      className="card-image"
                      imageName=""
                      size={150}
                      alt="Gift"
                      alternateImage={blank_profile_picture}
                    />
                    <div className="home-card-center-bold">
                      {home.christmas}
                    </div>
                  </>
                )}
                {event.kind === "MOTHER_DAY" && (
                  <>
                    <SquareImage
                      token={token}
                      className="card-image"
                      imageName=""
                      size={150}
                      alt="Gift"
                      alternateImage={blank_profile_picture}
                    />
                    <div className="home-card-center-bold">
                      {home.motherDay}
                    </div>
                  </>
                )}
                {event.kind === "FATHER_DAY" && (
                  <>
                    <SquareImage
                      token={token}
                      className="card-image"
                      imageName=""
                      size={150}
                      alt="Gift"
                      alternateImage={blank_profile_picture}
                    />
                    <div className="home-card-center-bold">
                      {home.fatherDay}
                    </div>
                  </>
                )}
                {event.kind === "BIRTHDAY" && (
                  <>
                    <SquareImage
                      token={token}
                      className="card-image"
                      imageName={event.picture}
                      size={150}
                      alt="Gift"
                      alternateImage={blank_profile_picture}
                    />
                    <div className="home-card-center-bold">
                      {home.birthdayOf}
                    </div>
                    <div className="home-card-center-bold">{event.name}</div>
                  </>
                )}
                <div className="home-card-center-no-bold">
                  {new Date(event.date).toLocaleDateString()}
                </div>
                <div className="home-card-center-no-bold">
                  {home.in}{" "}
                  {Math.ceil(
                    (new Date(event.date).getTime() - new Date().getTime()) /
                      (1000 * 3600 * 24),
                  )}{" "}
                  {home.day}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    );
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default HomePage;
