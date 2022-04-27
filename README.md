# MyGift

## Disclaimer

Other websites already exist and do exactly what I plan to do. The idea here
is to develop it for training purpose. There is nothing serious behind. Feel
free to contact me if you want to participate in some way.

## Purpose

I want to have a website that allow to create a wishlist, share it with friends.
On top of that, I plan to have "events" that allow either some people to buy stuff
to one people (birthday) or people to offer gifts each other (christmas). Basically
you can say "I, or we want to buy this" and avoid two people buying it. Of course
what you will receive will be hide to you. As a buyer, you will also have the possibility
to have a "buy list".

As of now, the website is quite limited but allows you to:

- Create and manage your own wishlist
- Add friends and manage your friend list
- Access to your friend wishlist
- As a friend, be able to say I am interested, I want to buy, or I have bought a given gift
- A buy list that summarize all you have to buy and already bought
- As a friend, add/propose secretly a gift

## Things that need upgrade

You can check already booked issue here in GitHub but there is more.

# How to build and other

## Build

```bash
gradlew test jar
```

This command will only build jar individually and run the tests.

```bash
gradlew shadowJar
```

This command will package the two shadow jar of this application. It depends how you want to deploy the application (fat
jar or classpath).

There are two jars

- auth-server-<version>-all.jar: shadow jar for the authentication server;
- back-<version>-all.jar: shadow jar for backend. Note that this jar embedded also the front end;

## Generate JKS for Https & SSL support

- Download private key and ssl cert from your vendor
- Rename them with extension .pem
- https://ktor.io/quickstart/guides/ssl.html
    - openssl pkcs12 -export -out ./keystore.p12 -inkey ./key.pem -in cert.pem -name <name>
    - choose password, it will correspond to alias password
    - keytool -importkeystore -alias <alias> -destkeystore ./mygift.jks -srcstoretype PKCS12 -srckeystore ./keystore.p12
    - choose password, it will correspond to jks password

## Activate foreign-keys on sqlite3

To check if activated: "PRAGMA foreign_keys;"
To activate in case not activated: "PRAGMA foreign_keys = ON;"
