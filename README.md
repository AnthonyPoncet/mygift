# MyGift

[![Main workflow](https://github.com/AnthonyPoncet/mygift/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/AnthonyPoncet/mygift/actions/workflows/main.yml)

MyGift is a small social network allowing to share gift list. The general idea is 
- You create a list of gift you would like independently of an event
- You add friends that can see this list
- They can reserve gift in your list, and you can in their

## General design

The backend is done in Rust using Axum for the webserver and SQLite for the database. The front is done in Vue.

