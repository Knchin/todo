// POST /functions/v1/update-note
// Body: { publicId, name, description, content }
// Authorizes per capability model (see _shared/note-auth.ts), validates
// fields, and updates only editable fields (name, description, content).
// id/publicId/createdAt/passcodeHash are never modified. updated_at is set by
// the database trigger.

import { withSupabase } from "npm:@supabase/server@^1";
import { authorizeNoteMutation, toNote } from "../_shared/note-auth.ts";

export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const body = await req.json().catch(() => null);
    if (!body || typeof body !== "object") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "Invalid JSON body." } }, { status: 400 });
    }
    const { publicId, name, description, content } = body as {
      publicId: string;
      name: string;
      description: string;
      content: string;
    };
    if (typeof publicId !== "string" || typeof name !== "string" || typeof content !== "string") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "publicId, name and content are required." } }, { status: 400 });
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

    const authz = await authorizeNoteMutation(ctx as any, publicId, req.headers.get("authorization"));
    if ("error" in authz) return authz.error;

    const { data, error } = await ctx.supabaseAdmin
      .from("notes")
      .update({ name: nameT, description: descriptionT, content: contentT })
      .eq("public_id", publicId.trim())
      .select("*")
      .single();

    if (error) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not update the note." } }, { status: 500 });
    }

    return Response.json({ note: toNote(data as any) });
  }),
};
