// The channel between the hub and the Gateway Hub app of each phone: one outgoing WebSocket per phone
// (taken over from Stone's relay). The hub sends MCP messages for an app on that phone and waits for the
// answer. No request is run twice: a timeout tells the phone to cancel it.
import { ToolError } from "../modules/types.ts";

export interface ChannelSocket { send(data: string): void }

/** What the rest of the hub sees of the channel. */
export interface Channel {
  online(device: string): boolean;
  /** Sends one JSON-RPC message to [app] on [device]; resolves with the app's JSON-RPC answer. */
  request(device: string, app: string, message: unknown, timeoutMs: number): Promise<any>;
}

export const MAX_CHANNEL_BYTES = 1 << 20;
const MAX_PENDING = 72;

interface Waiting { device: string; resolve(reply: unknown): void; reject(error: Error): void; timer: ReturnType<typeof setTimeout> }

export class ChannelCore implements Channel {
  private pending = new Map<string, Waiting>();
  private next = 1;

  private socketOf: (device: string) => ChannelSocket | undefined;
  private onRegister: (device: string, message: Record<string, unknown>) => Promise<void>;

  constructor(socketOf: (device: string) => ChannelSocket | undefined,
    onRegister: (device: string, message: Record<string, unknown>) => Promise<void>) {
    this.socketOf = socketOf; this.onRegister = onRegister;
  }

  online(device: string) { return !!this.socketOf(device); }

  request(device: string, app: string, message: unknown, timeoutMs: number): Promise<any> {
    const socket = this.socketOf(device);
    if (!socket) return Promise.reject(new ToolError("PROVIDER_OFFLINE", "The phone is not connected to the hub (open Gateway Hub on it)."));
    if (this.pending.size >= MAX_PENDING) return Promise.reject(new ToolError("BUSY", "The phone is busy. Try again soon."));
    const id = String(this.next++);
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        if (!this.pending.delete(id)) return;
        try { socket.send(JSON.stringify({ t: "cancel", id })); } catch { /* gone */ }
        reject(new ToolError("PROVIDER_TIMEOUT", "The app on the phone did not answer in time; the request was cancelled."));
      }, timeoutMs);
      this.pending.set(id, { device, resolve, reject, timer });
      try { socket.send(JSON.stringify({ t: "req", id, app, message })); } catch {
        clearTimeout(timer); this.pending.delete(id);
        reject(new ToolError("PROVIDER_OFFLINE", "The phone disconnected."));
      }
    });
  }

  /** One message from [device]: an answer ("res") or its list of apps and packages ("register"). */
  async receive(device: string, data: string | ArrayBuffer): Promise<void> {
    const text = typeof data === "string" ? data : new TextDecoder().decode(data);
    if (text.length > MAX_CHANNEL_BYTES) return;
    let message: Record<string, unknown>;
    try { message = JSON.parse(text); } catch { return; }
    if (typeof message !== "object" || message === null) return;
    if (message.t === "res") {
      const waiting = this.pending.get(String(message.id));
      // An answer only counts from the phone the request went to.
      if (!waiting || waiting.device !== device) return;
      this.pending.delete(String(message.id)); clearTimeout(waiting.timer); waiting.resolve(message.reply);
    } else if (message.t === "register") {
      await this.onRegister(device, message).catch((e) => console.warn(JSON.stringify({ event: "register_failed", device, error: String(e) })));
    }
  }

  /** The phone's socket closed: everything waiting for it fails at once. */
  closed(device: string) {
    for (const [id, w] of this.pending) if (w.device === device) {
      clearTimeout(w.timer); this.pending.delete(id); w.reject(new ToolError("PROVIDER_OFFLINE", "The phone disconnected."));
    }
  }
}
