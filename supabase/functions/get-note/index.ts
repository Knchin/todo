// GET /functions/v1/get-note?publicId=<id>
// For a PUBLIC note: returns the full note. For a PROTECTED note: returns only
// a locked stub (no name/content/timestamps), never the passcode hash. The
// protected content is only revealed via unlock-note.

import { withSupabase } from "npm:@supabase/server@^1";

export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const url = new URL(req.url);
    const publicId = url.searchParams.get("publicId")?.trim();
    if (!publicId) {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "publicId is required." } }, { status: 400 });
    }

    const { data, error } = await ctx.supabaseAdmin
      .from("notes")
      .select("id, public_id, type, name, description, content, created_at, updated_at")
      .eq("public_id", publicId)
      .maybeSingle();

    if (error) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not load the note." } }, { status: 500 });
    }
    if (!data) {
      return Response.json({ error: { code: "NOT_FOUND", message: "Note not found." } }, { status: 404 });
    }

    if (data.type === "PROTECTED") {
      return Response.json({ locked: true, publicId });
    }

    return Response.json({ note: toNote(data) });
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
