// Phones allowed to open the channel. Each gets its own random credential, shown once to the Gateway
// Hub app (created with the owner token); the hub keeps only its SHA-256.
import { type HubStoreApi, ToolError } from "../modules/types.ts";
import { newId, newSecret, sha256Hex } from "../agents.ts";
import { sameSecret } from "../auth.ts";

export interface Device { id: string; name: string; hash: string; created: number }
export const MAX_DEVICES = 10;
const KEY = "channel.devices";

export async function listDevices(store: HubStoreApi): Promise<Device[]> {
  try { return JSON.parse(await store.getConfig(KEY) ?? "[]") as Device[]; } catch { return []; }
}

export async function createDevice(store: HubStoreApi, rawName: string): Promise<{ device: Omit<Device, "hash">; secret: string }> {
  const name = String(rawName ?? "").trim().slice(0, 40) || "Teléfono";
  const devices = await listDevices(store);
  if (devices.length >= MAX_DEVICES) throw new ToolError("LIMIT", `Máximo ${MAX_DEVICES} teléfonos.`);
  const secret = newSecret("dev", await store.getConfig("space.id"));
  const device: Device = { id: newId(), name, hash: await sha256Hex(secret), created: Date.now() };
  await store.setConfig(KEY, JSON.stringify([...devices, device]));
  return { device: { id: device.id, name, created: device.created }, secret };
}

export async function deleteDevice(store: HubStoreApi, id: string): Promise<boolean> {
  const devices = await listDevices(store);
  if (!devices.some((d) => d.id === id)) return false;
  await store.setConfig(KEY, JSON.stringify(devices.filter((d) => d.id !== id)));
  return true;
}

/** The device whose credential is [secret], or null. */
export async function deviceFor(store: HubStoreApi, secret: string): Promise<Device | null> {
  if (!secret.startsWith("dev_")) return null;
  const hash = await sha256Hex(secret);
  for (const d of await listDevices(store)) if (await sameSecret(hash, d.hash)) return d;
  return null;
}
