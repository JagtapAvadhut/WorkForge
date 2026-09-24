/**
 * Capture WorkForge UI screenshots for FINAL_API_AUDIT.md
 * Requires frontend on :5173 and backend on :8080
 */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.resolve(__dirname, '../docs/screenshots');
const BASE = 'http://localhost:5173';
const USER = 'seedadmin';
const PASS = 'SeedPass123!';

async function shot(page, name, fullPage = true) {
  const file = path.join(OUT, `${name}.png`);
  await page.screenshot({ path: file, fullPage });
  console.log('saved', file);
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });
  const page = await context.newPage();

  // Login
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await shot(page, '01-login', false);

  // Fill login - try common selectors
  const userInput = page.locator('input[name="usernameOrEmail"], input[type="email"], input[name="login"], input[placeholder*="email" i], input[placeholder*="username" i]').first();
  const passInput = page.locator('input[type="password"]').first();
  await userInput.fill(USER);
  await passInput.fill(PASS);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(/dashboard|projects|my-work/, { timeout: 20000 }).catch(() => {});
  await page.waitForTimeout(1500);
  await shot(page, '02-dashboard');

  // Projects
  await page.goto(`${BASE}/projects`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, '03-projects');

  // Project detail / issues
  await page.goto(`${BASE}/projects/MWS`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, '04-project-detail');

  await page.goto(`${BASE}/projects/MWS/issues`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, '05-issue-list');

  await page.goto(`${BASE}/issues/MWS-1`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await shot(page, '06-issue-detail');

  // Create issue modal if button exists
  await page.goto(`${BASE}/projects/MWS/issues`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const createBtn = page.getByRole('button', { name: /create/i }).first();
  if (await createBtn.count()) {
    await createBtn.click().catch(() => {});
    await page.waitForTimeout(800);
    await shot(page, '07-create-issue', false);
    await page.keyboard.press('Escape').catch(() => {});
  }

  await page.goto(`${BASE}/projects/MWS/board`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '08-board');

  await page.goto(`${BASE}/projects/MWS/backlog`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);
  await shot(page, '09-backlog');

  // Sprints via backlog or picker
  await page.goto(`${BASE}/sprints`, { waitUntil: 'networkidle' }).catch(async () => {
    await page.goto(`${BASE}/projects/MWS/backlog`, { waitUntil: 'networkidle' });
  });
  await page.waitForTimeout(1000);
  await shot(page, '10-sprints');

  // Global search - open with Ctrl+K or click search
  await page.goto(`${BASE}/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const search = page.locator('input[placeholder*="Search" i], input[type="search"]').first();
  if (await search.count()) {
    await search.click();
    await search.fill('MWS');
    await page.waitForTimeout(1000);
    await shot(page, '11-search', false);
  } else {
    await page.keyboard.press('Control+K').catch(() => {});
    await page.waitForTimeout(500);
    const overlay = page.locator('input').first();
    await overlay.fill('MWS').catch(() => {});
    await page.waitForTimeout(800);
    await shot(page, '11-search', false);
  }

  await page.goto(`${BASE}/notifications`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, '12-notifications');

  await page.goto(`${BASE}/my-work`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, '13-my-work');

  await page.goto(`${BASE}/filters`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await shot(page, '14-filters');

  // Swagger evidence
  await page.goto('http://localhost:8080/swagger-ui.html', { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  await shot(page, '15-swagger', false);

  await browser.close();
  console.log('Done. Screenshots in', OUT);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
