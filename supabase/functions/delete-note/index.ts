// POST /functions/v1/delete-note
// Body: { publicId }
// Authorizes per capability model (see _shared/note-auth.ts) and deletes the
// note. After deletion, get-note returns NOT_FOUND.

import { withSupabase } from "npm:@supabase/server@^1";
import { authorizeNoteMutation } from "../_shared/note-auth.ts";

export default {
  fetch: withSupabase({ auth: "none" }, async (req, ctx) => {
    const body = await req.json().catch(() => null);
    if (!body || typeof body !== "object") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "Invalid JSON body." } }, { status: 400 });
    }
    const { publicId } = body as { publicId: string };
    if (typeof publicId !== "string") {
      return Response.json({ error: { code: "INVALID_REQUEST", message: "publicId is required." } }, { status: 400 });
    }

    const authz = await authorizeNoteMutation(ctx as any, publicId, req.headers.get("authorization"));
    if ("error" in authz) return authz.error;

    const { error } = await ctx.supabaseAdmin
      .from("notes")
      .delete()
      .eq("public_id", publicId.trim());

    if (error) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not delete the note." } }, { status: 500 });
    }

    return Response.json({ deleted: true });
  }),
};
