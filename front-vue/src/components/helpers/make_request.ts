import { useUserStore } from "@/stores/user";
import { useRouter } from "vue-router";
import { getBaseUrl } from "@/components/helpers/base_url";

export async function make_authorized_request(
  path: string,
  method: string = "get",
  body: BodyInit | null = null,
  isJson: boolean = true,
): Promise<Response | null> {
  const user = useUserStore().user;
  if (user !== null) {
    const headers: Record<string, string> = { Authorization: `Bearer ${user.token}` };
    if (body !== null && isJson) {
      headers["Content-Type"] = "application/json";
    }

    const requestInit: RequestInit = {
      method: method,
      headers: headers,
    };

    if (body !== null) {
      requestInit.body = body;
    }

    const response = await fetch(`${getBaseUrl()}${path}`, requestInit);

    if (response.status === 401) {
      useUserStore().logout();
      useRouter().push({ name: "home" });
    } else if (response.ok) {
      return response;
    }
  } else {
    useRouter().push({ name: "home" });
  }

  return null;
}
