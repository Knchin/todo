// Shared authorization for note mutation (update/delete).
//
// Capability model:
//  - PUBLIC notes: possession of the high-entropy publicId (the URL) is the
//    capability. No extra token needed.
//  - PROTECTED notes: require a short-lived signed token obtained from
//    unlock-note. The token is bound to the note id and verified server-side.

import { verifyNoteToken } from "../_shared/token.ts";

export interface NoteRow {
  id: string;
  public_id: string;
  type: string;
  name: string;
  description: string;
  content: string;
  created_at: string;
  updated_at: string;
}

/**
 * Returns the note row if the caller is authorized to mutate it, or a
 * Response (error) if not. Uses ctx.supabaseAdmin to read (bypasses RLS).
 */
export async function authorizeNoteMutation(
  ctx: { supabaseAdmin: { from: (t: string) => any } },
  publicId: string,
  authorizationHeader: string | null,
): Promise<{ note: NoteRow } | { error: Response }> {
  const { data, error } = await ctx.supabaseAdmin
    .from("notes")
    .select("id, public_id, type, name, description, content, created_at, updated_at")
    .eq("public_id", publicId.trim())
    .maybeSingle();

  if (error) {
    return { error: Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not load the note." } }, { status: 500 }) };
  }
  if (!data) {
    return { error: Response.json({ error: { code: "NOT_FOUND", message: "Note not found." } }, { status: 404 }) };
  }

  if (data.type === "PUBLIC") {
    return { note: data as unknown as NoteRow };
  }

  // PROTECTED note: require a valid signed token.
  const token = authorizationHeader?.replace(/^Bearer\s+/i, "");
  const secret = Deno.env.get("NOTE_TOKEN_SECRET") ?? "";
  const noteId = await verifyNoteToken(token, secret);
  if (!noteId || noteId !== data.id) {
    return { error: Response.json({ error: { code: "FORBIDDEN", message: "Unlock the note before modifying it." } }, { status: 403 }) };
  }
  return { note: data as unknown as NoteRow };
}

export function toNote(row: NoteRow) {
  return {
    id: row.id,
    publicId: row.public_id,
    type: row.type,
    name: row.name,
    description: row.description ?? "",
    content: row.content,
    createdAt: new Date(row.created_at).getTime(),
    updatedAt: new Date(row.updated_at).getTime(),
  };
}
