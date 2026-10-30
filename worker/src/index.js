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
function dayStr(d) {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Tehran" }).format(d);
}
function today() {
  return dayStr(new Date());
}
// the last `n` Tehran day-strings, most recent first (n<=28 here)
function lastDays(n) {
  const now = Date.now();
  const out = [];
  for (let k = 0; k < n; k++) out.push(dayStr(new Date(now - k * 86400000)));
  return out;
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
      const days28 = lastDays(28);
      const day = days28[0];
      // per-window day sets (most-recent-first slices of the 28-day list)
      const sets = { d7: new Set(days28.slice(0, 7)), d14: new Set(days28.slice(0, 14)),
                     d21: new Set(days28.slice(0, 21)), d28: new Set(days28) };
      const cut28 = days28[days28.length - 1];

      // lifetime totals + first/last per app
      const base = (await env.DB.prepare(
        `SELECT id, count AS total, first_at, last_at FROM clicks ORDER BY count DESC`
      ).all()).results;
      // every daily row within the last 28 days (lexical compare works on YYYY-MM-DD)
      const dailyRows = (await env.DB.prepare(
        `SELECT id, day, count FROM daily WHERE day >= ?1`
      ).bind(cut28).all()).results;

      const win = {};
      for (const r of base) win[r.id] = { today: 0, d7: 0, d14: 0, d21: 0, d28: 0 };
      for (const r of dailyRows) {
        const w = win[r.id] || (win[r.id] = { today: 0, d7: 0, d14: 0, d21: 0, d28: 0 });
        if (r.day === day) w.today += r.count;
        if (sets.d7.has(r.day)) w.d7 += r.count;
        if (sets.d14.has(r.day)) w.d14 += r.count;
        if (sets.d21.has(r.day)) w.d21 += r.count;
        if (sets.d28.has(r.day)) w.d28 += r.count;
      }
      const apps = base.map((r) => ({ ...r, ...win[r.id] }));
      const sum = (k) => apps.reduce((s, a) => s + (a[k] || 0), 0);
      return json({
        ok: true, day,
        total: apps.reduce((s, a) => s + a.total, 0),
        today: sum("today"), d7: sum("d7"), d14: sum("d14"), d21: sum("d21"), d28: sum("d28"),
        apps,
      });
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
