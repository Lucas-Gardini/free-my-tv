const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const root = process.argv[2];
const catalogs = {};
for (const file of fs.readdirSync('app/assets/i18n')) {
  if (file.endsWith('.json')) catalogs[file.slice(0, -5)] = JSON.parse(fs.readFileSync('app/assets/i18n/' + file, 'utf8'));
}
const read = name => fs.readFileSync(root ? path.join(root, name) : 'web/' + name, 'utf8');
const ipNode = {};
let ipEnabled = true, wallpaperEnabled = true, ipRefreshes = 0, wallpaperUpdates = 0;
const context = {window: {I18N_CATALOGUES: catalogs}, navigator: {language: 'en-US'},
  document: {documentElement: {}}, Intl: undefined, localIp: '', $: () => ipNode,
  pickedLanguage: () => 'auto', Data: {currency: 'USD'},
  Prefs: {temperature: () => 'auto', theme: () => 'auto', weather: () => true, quotes: () => true,
    coin: () => false, showIp: () => ipEnabled, liveWallpaper: () => wallpaperEnabled,
    set: (key, value) => { if (key === 'show_ip') ipEnabled = value; if (key === 'live_wallpaper') wallpaperEnabled = value; }},
  refreshLocalIp: () => ipRefreshes++, updateWallpaper: () => wallpaperUpdates++};
vm.createContext(context);
if (root) vm.runInContext(read('i18n.js'), context);
vm.runInContext(read('strings.js'), context);
const home = read('home.js');
for (const name of ['renderLocalIp', 'settingRows']) {
  const fn = home.match(new RegExp('^  function ' + name + '\\([^]*?^  }', 'm'));
  assert.ok(fn, name + ' is present');
  vm.runInContext(fn[0], context);
}
let checks = 0;
function equal(actual, expected, label) { assert.equal(actual, expected, label); checks++; }
for (const [lang, catalog] of Object.entries(catalogs)) {
  context.I18N.set(lang);
  const rows = context.settingRows();
  equal(rows[4].label, catalog['Show TV IP address'], lang + ' IP toggle');
  equal(rows[5].label, catalog['Live wallpaper'], lang + ' wallpaper toggle');
  equal(rows[6].label, catalog['Quotes widget'], lang + ' quotes toggle');
  context.renderLocalIp();
  equal(ipNode.textContent, ' · ' + catalog['IP unavailable'], lang + ' unavailable IP');
}
context.I18N.set('en');
context.settingRows()[4].step();
context.renderLocalIp();
equal(ipNode.textContent, '', 'IP toggle hides the label');
equal(ipRefreshes, 1, 'IP toggle refreshes the address');
context.settingRows()[5].step();
equal(wallpaperEnabled, false, 'wallpaper toggle persists off');
equal(wallpaperUpdates, 1, 'wallpaper toggle refreshes playback');
console.log('Web customization translations: ' + checks + ' checks passed');
