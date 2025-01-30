export function getBaseUrl(): string {
  const protocol = window.location.protocol;
  const hostname = window.location.hostname;
  let port;
  if (import.meta.env.PROD) {
    port = window.location.port;
  } else {
    port = "4242";
  }

  if (port.length !== 0) {
    port = ":" + port;
  }

  return protocol + "//" + hostname + port + "/api";
}
