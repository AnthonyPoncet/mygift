import React from "react";

import { useNavigate } from "react-router-dom";

import { useAppSelector } from "../redux/store";
import { selectMessages } from "../redux/reducers/locale";
import { selectSignIn } from "../redux/reducers/signin";

import tree_green from "./image/christmas-easter/christmas-tree-green.png";
import selected_tree from "./image/christmas-easter/christmas-tree-selected.png";

import { isMobile } from "react-device-detect";

function HomePage() {
  const username = useAppSelector(selectSignIn).username;
  const home = useAppSelector(selectMessages).home;

  let navigate = useNavigate();

  var days = Math.ceil((new Date("12/25/2023").getTime() - new Date().getTime()) / (1000 * 3600 * 24));

  if (username) {
      if (isMobile) {
        return (
          <div style={{margin: "10px", display: "flex", flexWrap: "wrap", justifyContent: "center"}}>
            <h3 style={{flexBasis: "100%", textAlign: "center"}}>
              {home.hello} {username}
            </h3>
            <div style={{ display: "flex"}}>
                <img style={{ width: "50%"}} src={tree_green} />
                <img style={{ width: "50%"}} src={selected_tree} />
            </div>
            <span className="christmas-text"> { days } {home.days_before_christmas} </span>
            <div style={{ display: "flex"}}>
                <img style={{ width: "50%"}} src={selected_tree} />
                <img style={{ width: "50%"}} src={tree_green} />
            </div>
          </div>
        );
      } else {
        return (
          <div style={{margin: "10px", display: "flex", flexWrap: "wrap", justifyContent: "center"}}>
            <h3 style={{flexBasis: "100%", textAlign: "center"}}>
              {home.hello} {username}
            </h3>
            <div className="christmas-home">
                <img src={tree_green} />
                <img src={selected_tree} />
                <span className="christmas-text"> { days } {home.days_before_christmas} </span>
                <img src={selected_tree} />
                <img src={tree_green} />
            </div>
          </div>
        );
      }
  } else {
    console.log("Unauthorized... Redirecting...");
    navigate("../signin");
    return <div></div>;
  }
}

export default HomePage;
