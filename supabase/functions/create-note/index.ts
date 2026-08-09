// POST /functions/v1/create-note
// Body: { name, description, content, type, passcode? }
// Creates a note. For PROTECTED notes, hashes the passcode with bcrypt.
// Public identifiers are generated server-side with Web Crypto (not client
// supplied), keeping URL capabilities unpredictable.

import { withSupabase } from "npm:@supabase/server@^1";
import { hash } from "jsr:@felix/bcrypt@2.1.0";

export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const body = await req.json().catch(() => null);
    if (!body || typeof body !== "object") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "Invalid JSON body." } }, { status: 400 });
    }

    const { name, description = "", content, type, passcode = null } = body as {
      name: string;
      description?: string;
      content: string;
      type: string;
      passcode?: string | null;
    };

    if (typeof name !== "string" || typeof content !== "string" || typeof type !== "string") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "name, content and type are required." } }, { status: 400 });
    }
    if (type !== "PUBLIC" && type !== "PROTECTED") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "type must be PUBLIC or PROTECTED." } }, { status: 400 });
    }

    const nameT = name.trim();
    const contentT = content.trim();
    const descriptionT = description ?? "";

    if (nameT.length === 0 || nameT.length > 200) {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "Name must be 1-200 characters." } }, { status: 400 });
    }
    if (descriptionT.length > 2000) {
      return Response.json({ error: { code: "CONTENT_TOO_LARGE", message: "Description is too long." } }, { status: 413 });
    }
    if (contentT.length === 0 || contentT.length > 20000) {
      return Response.json({ error: { code: "CONTENT_TOO_LARGE", message: "Content must be 1-20000 characters." } }, { status: 413 });
    }

    let passcodeHash: string | null = null;
    if (type === "PROTECTED") {
      if (typeof passcode !== "string" || passcode.length < 4 || passcode.length > 128) {
        return Response.json({ error: { code: "INVALID_REQUEST", message: "A valid passcode (4-128 chars) is required." } }, { status: 400 });
      }
      passcodeHash = await hash(passcode, 12);
    }

    const publicId = cryptoRandomId();

    const { data, error } = await ctx.supabaseAdmin
      .from("notes")
      .insert({
        public_id: publicId,
        type,
        name: nameT,
        description: descriptionT,
        content: contentT,
        passcode_hash: passcodeHash,
      })
      .select("*")
      .single();

    if (error) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not create the note." } }, { status: 500 });
    }

    return Response.json({
      note: toNote(data),
      publicUrl: publicId,
    }, { status: 201 });
  }),
};

function cryptoRandomId(): string {
  const bytes = new Uint8Array(12);
  crypto.getRandomValues(bytes);
  const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
  let out = "";
  for (const b of bytes) out += alphabet[b % alphabet.length];
  return out;
}

function toNote(row: Record<string, unknown>) {
  return {
    id: row.id,
    publicId: row.public_id,
    type: row.type,
    name: row.name,
    description: row.description ?? "",
    content: row.content,
    createdAt: new Date(row.created_at as string).getTime(),
    updatedAt: new Date(row.updated_at as string).getTime(),
  };
}
