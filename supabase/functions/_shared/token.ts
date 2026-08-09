// Short-lived signed token issued after a protected note is unlocked.
// HS256 JWT signed with a dedicated server secret. Never contains the
// passcode or passcode hash. The client sends this back in the Authorization
// header to prove it has successfully unlocked the note.

const TOKEN_TTL_SECONDS = 60 * 60; // 1 hour

const encoder = new TextEncoder();

function toBase64Url(input: Uint8Array | string): string {
  const bytes = typeof input === "string" ? encoder.encode(input) : input;
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function fromBase64Url(input: string): Uint8Array {
  const b64 = input.replace(/-/g, "+").replace(/_/g, "/");
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}

async function importKey(secret: string): Promise<CryptoKey> {
  return crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign", "verify"],
  );
}

export interface NoteToken {
  noteId: string;
  exp: number;
}

export async function signNoteToken(noteId: string, secret: string): Promise<string> {
  const header = { alg: "HS256", typ: "JWT" };
  const payload: NoteToken = { noteId, exp: Math.floor(Date.now() / 1000) + TOKEN_TTL_SECONDS };
  const signingInput = `${toBase64Url(JSON.stringify(header))}.${toBase64Url(JSON.stringify(payload))}`;
  const key = await importKey(secret);
  const sig = await crypto.subtle.sign("HMAC", key, encoder.encode(signingInput));
  return `${signingInput}.${toBase64Url(new Uint8Array(sig))}`;
}

/**
 * Verifies signature and expiry. Returns the noteId on success, or null.
 * A missing/invalid/expired token returns null.
 */
export async function verifyNoteToken(
  token: string | undefined,
  secret: string,
): Promise<string | null> {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [headerB64, payloadB64, sigB64] = parts;
  const signingInput = `${headerB64}.${payloadB64}`;
  try {
    const key = await importKey(secret);
    const valid = await crypto.subtle.verify(
      "HMAC",
      key,
      fromBase64Url(sigB64),
      encoder.encode(signingInput),
    );
    if (!valid) return null;
    const payload = JSON.parse(fromBase64Url(payloadB64).reduce(
      (acc, b) => acc + String.fromCharCode(b),
      "",
    )) as NoteToken;
    if (typeof payload.noteId !== "string" || typeof payload.exp !== "number") return null;
    if (payload.exp < Math.floor(Date.now() / 1000)) return null;
    return payload.noteId;
  } catch {
    return null;
  }
}
