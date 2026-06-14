import { Client } from "@stomp/stompjs";

let stompClient: Client | null = null;

export const getStompClient = () => {
  if (!stompClient) {
    stompClient = new Client({
      brokerURL: "ws://localhost:8080/ws",
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => console.log("Connected to STOMP"),
      onStompError: (frame) => console.error("STOMP error:", frame),
    });
    stompClient.activate();
  }
  return stompClient;
};
