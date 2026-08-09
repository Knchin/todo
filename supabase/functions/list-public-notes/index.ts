// GET /functions/v1/list-public-notes
// Returns a lightweight list of PUBLIC notes for the homepage, ordered by most
// recently updated. Protected notes are never returned (the SQL filters by
// type = 'PUBLIC'), so protected metadata cannot leak.

import { withSupabase } from "npm:@supabase/server@^1";

export default {
  fetch: withSupabase({ auth: "none" }, async (_req, ctx) => {
    const { data, error } = await ctx.supabaseAdmin
      .from("notes")
      .select("public_id, name, description, updated_at")
      .eq("type", "PUBLIC")
      .order("updated_at", { ascending: false })
      .limit(100);

    if (error) {
      return Response.json({ error: { code: "INTERNAL_ERROR", message: "Could not load notes." } }, { status: 500 });
    }

    const notes = (data ?? []).map((row: Record<string, unknown>) => ({
      publicId: row.public_id,
      name: row.name,
      description: row.description ?? "",
      updatedAt: new Date(row.updated_at as string).getTime(),
    }));

    return Response.json({ notes });
  }),
};
