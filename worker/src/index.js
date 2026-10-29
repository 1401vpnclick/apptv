// Offline join-click counter (Cloudflare Worker + D1)
//
//   GET  /hit?id=<package>          public — called by OfflineDialog when "join" is tapped
//   GET  /stats                     admin  — per-app totals (Authorization: Bearer <ADMIN_KEY>)
//   POST /reset?id=<package>        admin  — delete one app's counts
//
// ADMIN_KEY is a Worker secret (set by the GitHub Actions deploy workflow).

const ID_RE = /^[A-Za-z0-9._-]{1,120}$/;

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Access-Control-Max-Age": "86400",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store", ...CORS },
  });
}

// Tehran calendar day, so "today" matches the user's day
function today() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Tehran" }).format(new Date());
}

function isAdmin(req, env) {
  const key = env.ADMIN_KEY || "";
  const h = req.headers.get("Authorization") || "";
  return key.length > 0 && h === "Bearer " + key;
}

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    const path = url.pathname.replace(/\/+$/, "") || "/";

    if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });

    if (path === "/hit") {
      const id = (url.searchParams.get("id") || "").trim();
      if (!ID_RE.test(id)) return json({ ok: false, error: "bad id" }, 400);
      const now = Date.now();
      const day = today();
      await env.DB.batch([
        env.DB.prepare(
          `INSERT INTO clicks (id, count, first_at, last_at) VALUES (?1, 1, ?2, ?2)
           ON CONFLICT(id) DO UPDATE SET count = count + 1, last_at = ?2`
        ).bind(id, now),
        env.DB.prepare(
          `INSERT INTO daily (id, day, count) VALUES (?1, ?2, 1)
           ON CONFLICT(id, day) DO UPDATE SET count = count + 1`
        ).bind(id, day),
      ]);
      return json({ ok: true });
    }

    if (path === "/stats") {
      if (!isAdmin(req, env)) return json({ ok: false, error: "unauthorized" }, 401);
      const day = today();
      const { results } = await env.DB.prepare(
        `SELECT c.id, c.count AS total, c.first_at, c.last_at,
                COALESCE(d.count, 0) AS today
         FROM clicks c LEFT JOIN daily d ON d.id = c.id AND d.day = ?1
         ORDER BY c.count DESC`
      ).bind(day).all();
      const total = results.reduce((s, r) => s + r.total, 0);
      const todayTotal = results.reduce((s, r) => s + r.today, 0);
      return json({ ok: true, day, total, today: todayTotal, apps: results });
    }

    if (path === "/reset" && req.method === "POST") {
      if (!isAdmin(req, env)) return json({ ok: false, error: "unauthorized" }, 401);
      const id = (url.searchParams.get("id") || "").trim();
      if (!ID_RE.test(id)) return json({ ok: false, error: "bad id" }, 400);
      await env.DB.batch([
        env.DB.prepare("DELETE FROM clicks WHERE id = ?1").bind(id),
        env.DB.prepare("DELETE FROM daily WHERE id = ?1").bind(id),
      ]);
      return json({ ok: true });
    }

    if (path === "/") return json({ ok: true, service: "offline-counter" });
    return json({ ok: false, error: "not found" }, 404);
  },
};
