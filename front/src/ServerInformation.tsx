export function getServerUrl(): string {
  let protocol = window.location.protocol;
  let hostname = window.location.hostname;
  let port = 8080;

  return protocol + "//" + hostname + ":" + port;
}
