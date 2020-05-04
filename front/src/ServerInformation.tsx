
export function getServerUrl() : string {
    let hostname = window.location.hostname;
    let port = window.location.port;
    return "http://"+hostname+":"+"8080";
}
