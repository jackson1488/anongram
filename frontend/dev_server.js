const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const CLIENT_HTML = path.join(__dirname, 'mockups', 'anongram_app_prototype.html');

let clients = [];

// Watch mockups and assets for live-reload
fs.watch(path.join(__dirname, 'mockups'), { recursive: true }, (eventType, filename) => {
  if (filename) {
    console.log(`[Hot-Reload] File changed: ${filename}, notifying browser clients...`);
    clients.forEach(res => res.write(`data: reload\n\n`));
  }
});

const server = http.createServer((req, res) => {
  // SSE endpoint for instant hot reload
  if (req.url === '/events') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*'
    });
    clients.push(res);
    req.on('close', () => {
      clients = clients.filter(c => c !== res);
    });
    return;
  }

  // Serve static assets from frontend/
  let filePath = path.join(__dirname, req.url === '/' ? 'mockups/anongram_app_prototype.html' : req.url.replace(/^\//, ''));

  if (!fs.existsSync(filePath)) {
    // Try mockups/
    filePath = path.join(__dirname, 'mockups', req.url.replace(/^\//, ''));
  }

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const mimeTypes = {
      '.html': 'text/html; charset=utf-8',
      '.js': 'application/javascript',
      '.css': 'text/css',
      '.svg': 'image/svg+xml',
      '.png': 'image/png',
      '.jpg': 'image/jpeg',
      '.json': 'application/json'
    };

    let content = fs.readFileSync(filePath);
    if (ext === '.html') {
      // Inject SSE live-reload script into HTML
      const reloadScript = `
        <script>
          const es = new EventSource('/events');
          es.onmessage = (e) => {
            if (e.data === 'reload') {
              console.log('[AnonGram Hot-Reload] Triggering live reload...');
              location.reload();
            }
          };
        </script>
      `;
      content = Buffer.from(content.toString('utf-8').replace('</body>', `${reloadScript}</body>`));
    }

    res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' });
    res.end(content);
    return;
  }

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`🚀 AnonGram Dev Hot-Reload Server running at: http://localhost:${PORT}`);
});
