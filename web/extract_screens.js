const fs = require('fs');
const path = require('path');

// Read files
const mobileHtml = fs.readFileSync('pawpop-mobile.html', 'utf8');
const desktopHtml = fs.readFileSync('pawpop-desktop.html', 'utf8');

// Ensure output directories exist
fs.mkdirSync('_stitch_ref/mobile', { recursive: true });
fs.mkdirSync('_stitch_ref/desktop', { recursive: true });

// 1. Mobile Screen Extraction
const mobileRegex = /<section class="phone-screen">([\s\S]*?)<\/section>/g;
let mMatch;
let mobileCount = 0;

while ((mMatch = mobileRegex.exec(mobileHtml)) !== null) {
  const content = mMatch[1];
  
  // Extract title
  const titleMatch = /<h3>([\s\S]*?)<\/h3>/.exec(content);
  const title = titleMatch ? titleMatch[1].trim() : `Mobile Screen ${mobileCount}`;
  // Normalize filename, e.g., "Auth / Splash" -> "auth_splash.html"
  const filename = title.toLowerCase()
    .replace(/\s*\/\s*/g, '_')
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '') + '.html';
  
  // Extract <div class="phone">...</div>
  const phoneMatch = /<div class="phone">([\s\S]*?<\/article>\s*<\/div>)/.exec(content);
  if (!phoneMatch) continue;
  const phoneHtml = phoneMatch[0];

  const pageHtml = `<!doctype html>
<html lang="zh-CN" data-theme="sugar" data-profile-bg="picnic">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>M&D Mobile - ${title}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&family=Noto+Sans+SC:wght@400;500;700;800&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,600,0,0" rel="stylesheet">
  <link rel="stylesheet" href="../../pawpop-mobile.css" />
</head>
<body style="margin: 0; padding: 20px; background: #fff7ee; display: flex; justify-content: center; align-items: center; min-height: 100vh;">
  <section class="phone-screen" style="margin: 0;">
    ${phoneHtml}
  </section>
  <script src="../../pawpop-mobile.js" defer></script>
</body>
</html>`;

  fs.writeFileSync(path.join('_stitch_ref/mobile', filename), pageHtml, 'utf8');
  mobileCount++;
}

console.log(`Extracted ${mobileCount} mobile screens.`);

// 2. Desktop Screen Extraction
const desktopRegex = /<section class="desktop-frame\s+([^"]+)">([\s\S]*?)<\/section>/g;
let dMatch;
let desktopCount = 0;

while ((dMatch = desktopRegex.exec(desktopHtml)) !== null) {
  const className = dMatch[1].trim();
  const innerContent = dMatch[2];
  
  // Extract title
  const titleMatch = /<b>([\s\S]*?)<\/b>/.exec(innerContent);
  const title = titleMatch ? titleMatch[1].trim() : `Desktop ${className}`;
  const filename = title.toLowerCase()
    .replace(/\s*\/\s*/g, '_')
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '') + '.html';

  const pageHtml = `<!doctype html>
<html lang="zh-CN" data-theme="sugar" data-profile-bg="picnic">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>M&D Desktop - ${title}</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Fredoka:wght@500;600;700&family=Noto+Sans+SC:wght@400;500;700;800&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,600,0,0" rel="stylesheet">
  <link rel="stylesheet" href="../../pawpop-desktop.css" />
</head>
<body style="margin: 0; padding: 20px; background: #fff7ee; display: flex; justify-content: center; align-items: center; min-height: 100vh;">
  <section class="desktop-frame ${className}" style="margin: 0; width: 100%; max-width: 1280px; min-height: 620px;">
    ${innerContent}
  </section>
  <script src="../../pawpop-desktop.js" defer></script>
</body>
</html>`;

  fs.writeFileSync(path.join('_stitch_ref/desktop', filename), pageHtml, 'utf8');
  desktopCount++;
}

console.log(`Extracted ${desktopCount} desktop screens.`);
