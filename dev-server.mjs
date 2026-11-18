import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { scanArgonModules, summarizeCategories } from './dev-server-core.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.DEV_PORT || 3000);
const HOST = process.env.HOST || '0.0.0.0';

function getWorkspaceSummary() {
  const modules = scanArgonModules(__dirname);
  const categories = summarizeCategories(modules);
  const compiledClassesDir = path.join(__dirname, 'build', 'classes', 'java', 'main');
  const hasCompiledClasses = fs.existsSync(compiledClassesDir);

  return {
    projectName: 'Argon Client',
    minecraftVersion: '1.12.2 (Eaglercraft + OptiFine)',
    buildTool: 'Gradle',
    hasCompiledClasses,
    totalModules: modules.length,
    categories,
    modules,
  };
}

function renderHtml(summary) {
  const initialData = JSON.stringify(summary).replace(/</g, '\\u003c');
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Argon Client — 1.12.2 Gradle Workspace</title>
  <style>
    :root {
      color-scheme: dark;
      --bg: #0f172a;
      --surface: #1e293b;
      --surface-alt: #162032;
      --border: #334155;
      --text: #f8fafc;
      --muted: #94a3b8;
      --accent: #38bdf8;
      --accent-soft: rgba(56, 189, 248, 0.14);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg);
      color: var(--text);
      line-height: 1.5;
    }
    .container {
      max-width: 1180px;
      margin: 0 auto;
      padding: 24px 20px 48px;
    }
    header {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: flex-end;
      gap: 16px;
      padding-bottom: 20px;
      border-bottom: 1px solid var(--border);
      margin-bottom: 24px;
    }
    h1 {
      margin: 0;
      font-size: 1.75rem;
      font-weight: 700;
      letter-spacing: -0.02em;
    }
    .subtitle {
      margin: 4px 0 0;
      color: var(--muted);
      font-size: 0.95rem;
    }
    .stats-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
      gap: 12px;
      margin-bottom: 24px;
    }
    .stat-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 14px 16px;
    }
    .stat-label {
      font-size: 0.78rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--muted);
    }
    .stat-value {
      margin-top: 4px;
      font-size: 1.25rem;
      font-weight: 600;
    }
    .commands {
      background: var(--surface-alt);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 16px;
      margin-bottom: 24px;
    }
    .commands h2 {
      margin: 0 0 10px;
      font-size: 1rem;
      font-weight: 600;
    }
    .cmd-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 10px;
    }
    .cmd-box {
      background: var(--bg);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 10px 12px;
    }
    .cmd-title {
      font-size: 0.8rem;
      color: var(--muted);
      margin-bottom: 4px;
    }
    code {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 0.85rem;
      color: var(--accent);
      word-break: break-all;
    }
    .controls {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
      margin-bottom: 16px;
    }
    .search-wrap {
      flex: 1 1 260px;
    }
    label.sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip: rect(0 0 0 0);
      white-space: nowrap;
    }
    input[type="search"] {
      width: 100%;
      background: var(--surface);
      color: var(--text);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 10px 12px;
      font-size: 0.95rem;
    }
    input[type="search"]:focus {
      outline: 2px solid var(--accent);
      outline-offset: 1px;
    }
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 18px;
    }
    .filter-btn {
      background: var(--surface);
      color: var(--text);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 7px 12px;
      font-size: 0.85rem;
      cursor: pointer;
    }
    .filter-btn[aria-pressed="true"] {
      background: var(--accent-soft);
      border-color: var(--accent);
      color: var(--accent);
      font-weight: 600;
    }
    .results-meta {
      margin-bottom: 12px;
      color: var(--muted);
      font-size: 0.9rem;
    }
    .module-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 12px;
    }
    .module-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 14px;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .module-name {
      font-size: 1rem;
      font-weight: 600;
      word-break: break-word;
    }
    .module-cat {
      font-size: 0.8rem;
      color: var(--accent);
      font-weight: 500;
    }
    .module-path {
      font-size: 0.78rem;
      color: var(--muted);
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      word-break: break-all;
    }
    .empty {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 24px;
      text-align: center;
      color: var(--muted);
    }
  </style>
</head>
<body>
  <div class="container">
    <header>
      <div>
        <h1>Argon Client Workspace</h1>
        <p class="subtitle">Minecraft 1.12.2 (Eaglercraft + OptiFine) Java &amp; Gradle Source Repository</p>
      </div>
    </header>

    <section class="stats-row" aria-label="Workspace overview">
      <div class="stat-card">
        <div class="stat-label">Target Platform</div>
        <div class="stat-value">Java 17 / LWJGL / TeaVM</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Argon Feature Modules</div>
        <div class="stat-value" id="total-count">${summary.totalModules}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">Rendering Pipeline</div>
        <div class="stat-value">LWJGL / GlStateManager</div>
      </div>
    </section>

    <section class="commands" aria-label="Gradle commands">
      <h2>Gradle Build &amp; Launch Commands</h2>
      <div class="cmd-grid">
        <div class="cmd-box">
          <div class="cmd-title">Compile Client</div>
          <code>./gradlew compileJava</code>
        </div>
        <div class="cmd-box">
          <div class="cmd-title">Full Distribution Build</div>
          <code>./gradlew clean build</code>
        </div>
        <div class="cmd-box">
          <div class="cmd-title">Run Desktop Runtime</div>
          <code>./gradlew run</code>
        </div>
      </div>
    </section>

    <main>
      <div class="controls">
        <div class="search-wrap">
          <label class="sr-only" for="module-search">Search modules</label>
          <input id="module-search" type="search" placeholder="Search Java modules by name, category, or path..." />
        </div>
        <button id="clear-filters" class="filter-btn" type="button">Reset Filters</button>
      </div>

      <nav class="filters" id="category-filters" aria-label="Filter modules by subsystem"></nav>
      <div class="results-meta" id="results-meta"></div>
      <div class="module-grid" id="module-grid"></div>
    </main>
  </div>

  <script>
    const DATA = ${initialData};
    let activeCategory = 'All';
    let searchQuery = '';

    const filtersEl = document.getElementById('category-filters');
    const gridEl = document.getElementById('module-grid');
    const metaEl = document.getElementById('results-meta');
    const searchEl = document.getElementById('module-search');
    const clearBtn = document.getElementById('clear-filters');

    function renderFilters() {
      const allBtn = document.createElement('button');
      allBtn.type = 'button';
      allBtn.className = 'filter-btn';
      allBtn.textContent = 'All (' + DATA.modules.length + ')';
      allBtn.setAttribute('aria-pressed', String(activeCategory === 'All'));
      allBtn.addEventListener('click', () => {
        activeCategory = 'All';
        renderFilters();
        renderModules();
      });
      filtersEl.innerHTML = '';
      filtersEl.appendChild(allBtn);

      for (const cat of DATA.categories) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'filter-btn';
        btn.textContent = cat.name + ' (' + cat.count + ')';
        btn.setAttribute('aria-pressed', String(activeCategory === cat.name));
        btn.addEventListener('click', () => {
          activeCategory = cat.name;
          renderFilters();
          renderModules();
        });
        filtersEl.appendChild(btn);
      }
    }

    function renderModules() {
      const q = searchQuery.trim().toLowerCase();
      const filtered = DATA.modules.filter((m) => {
        if (activeCategory !== 'All' && m.category !== activeCategory) return false;
        if (!q) return true;
        return (
          m.name.toLowerCase().includes(q) ||
          m.category.toLowerCase().includes(q) ||
          m.path.toLowerCase().includes(q)
        );
      });

      metaEl.textContent = 'Showing ' + filtered.length + ' of ' + DATA.modules.length + ' Java source files';
      gridEl.innerHTML = '';

      if (filtered.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'empty';
        empty.textContent = 'No modules match the active filters.';
        gridEl.appendChild(empty);
        return;
      }

      for (const item of filtered) {
        const card = document.createElement('article');
        card.className = 'module-card';

        const cat = document.createElement('div');
        cat.className = 'module-cat';
        cat.textContent = item.category;

        const name = document.createElement('div');
        name.className = 'module-name';
        name.textContent = item.fileName;

        const pathEl = document.createElement('div');
        pathEl.className = 'module-path';
        pathEl.textContent = item.path;

        card.appendChild(cat);
        card.appendChild(name);
        card.appendChild(pathEl);
        gridEl.appendChild(card);
      }
    }

    searchEl.addEventListener('input', (e) => {
      searchQuery = e.target.value;
      renderModules();
    });

    clearBtn.addEventListener('click', () => {
      activeCategory = 'All';
      searchQuery = '';
      searchEl.value = '';
      renderFilters();
      renderModules();
    });

    renderFilters();
    renderModules();
  </script>
</body>
</html>`;
}

const server = http.createServer((req, res) => {
  try {
    const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);

    if (url.pathname === '/health') {
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify({ ok: true }));
      return;
    }

    if (url.pathname === '/api/modules') {
      const summary = getWorkspaceSummary();
      res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      res.end(JSON.stringify(summary));
      return;
    }

    if (url.pathname === '/' || url.pathname === '/index.html') {
      const summary = getWorkspaceSummary();
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      res.end(renderHtml(summary));
      return;
    }

    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Not Found');
  } catch (err) {
    console.error('Request error:', err);
    res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('Internal Server Error');
  }
});

server.on('error', (err) => {
  console.error('Server error:', err);
  process.exitCode = 1;
});

server.listen(PORT, HOST, () => {
  console.log(`Argon workspace server listening on http://${HOST}:${PORT}`);
});

function shutdown() {
  server.close(() => process.exit(0));
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
