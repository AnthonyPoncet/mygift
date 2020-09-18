# MyGift
[![Build Status](https://travis-ci.org/AnthonyPoncet/mygift.svg?branch=master)](https://travis-ci.org/AnthonyPoncet/mygift)


## Disclaimer
Other websites already exist and do exactly what I plan to do. The idea here
is to develop it for training purpose. There is nothing serious behind. Feel
free to contact me if you want to participate in some way.

## Purpose
I want to have a website that allow to create a wishlist, share it with friends.
On top of that, I plan to have "events" that allow either some people to buy stuff
to one people (birthday) or people to offer gifts each other (christmas). Basically
you can say "I or we want to buy this" and avoid two people buying it. Of course
what you will receive will be hide to you. As a buyer, you will also have the possibility
to have a "buy list".

As of now, the website is quite limited but allows you to:
- Create and manage your own wishlist
- Add friend and manage your friend list
- Access to your friend wishlist
- As a friend, be able to say I am interested, I want to bought or I have bought a given gift
- A bought list that summarize all you have to buy
- As a friend, add/propose secretly a gift

## More to come
In the future, I plan to add (in that order):
- In Home page once connected, what's new or coming next or...
- A chat
- Mobile App
- UT/IT/...
- Security
- Offline profile (Doodle like)
- ...

## Things that need upgrade
Everything right now but my next work will be:
- Friend list UI (be able to unblock and better UI in general)
- Manage profile (such as modify password)
- At some stage, think if page should refresh on new gift/friend request/...
- A doc for REST end point and once all done, refactor them in meaningful way
- Refactor typescript

## Known bug

# How to build and other

## Build
Run gradlew jar. It will generate two jars. One for the back accessible by the external internet and another one
for the auth-server that should be accessible by the back only. Front is package inside the back jar.

## Generate JKS for Https & SSL support
 - Download private key and ssl cert
 - Rename them with extension .pem
 - https://ktor.io/quickstart/guides/ssl.html
   - openssl pkcs12 -export -out ./keystore.p12 -inkey ./key.pem -in cert.pem -name mygift
   - choose password, it will correspond to alias password
   - keytool -importkeystore -alias mygift -destkeystore ./mygift.jks -srcstoretype PKCS12 -srckeystore ./keystore.p12
   - choose password, it will correspond to jks password
 