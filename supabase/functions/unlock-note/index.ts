// POST /functions/v1/unlock-note
// Body: { publicId, passcode }
// Verifies the passcode (bcrypt) for a protected note. On success, returns the
// note content plus a short-lived signed token that authorizes subsequent
// updates/deletes. The passcode hash is never returned.

import { withSupabase } from "npm:@supabase/server@^1";
import { verify } from "jsr:@felix/bcrypt@2.1.0";
import { signNoteToken } from "../_shared/token.ts";

export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const body = await req.json().catch(() => null);
    if (!body || typeof body !== "object") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "Invalid JSON body." } }, { status: 400 });
    }
    const { publicId, passcode } = body as { publicId: string; passcode: string };
    if (typeof publicId !== "string" || typeof passcode !== "string") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "publicId and passcode are required." } }, { status: 400 });
    }

    const { data, error } = await ctx.supabaseAdmin
      .from("notes")
      .select("id, public_id, type, name, description, content, passcode_hash, created_at, updated_at")
      .eq("public_id", publicId.trim())
      .maybeSingle();

    if (error) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not load the note." } }, { status: 500 });
    }
    if (!data) {
      // Uniform response for missing note or wrong passcode reduces enumeration.
      return Response.json({ error: { code: "INVALID_PASSCODE", message: "Invalid passcode." } }, { status: 401 });
    }
    if (data.type !== "PROTECTED" || !data.passcode_hash) {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "This note is not passcode protected." } }, { status: 400 });
    }

    const valid = await verify(passcode, data.passcode_hash);
    if (!valid) {
      return Response.json({ error: { code: "INVALID_PASSCODE", message: "Invalid passcode." } }, { status: 401 });
    }

    const secret = Deno.env.get("NOTE_TOKEN_SECRET") ?? "";
    if (!secret) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Server not configured." } }, { status: 500 });
    }
    const token = await signNoteToken(data.id, secret);

    return Response.json({
      note: toNote(data),
      token,
    });
  }),
};

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
